package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.model.ExtractionRecord
import no.fdk.concept_catalog.rdf.extract
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.repository.ImportResultRepository
import no.fdk.concept_catalog.validation.validateSchema
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.riot.Lang
import org.apache.jena.shared.JenaException
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.SKOS
import org.openapi4j.core.validation.ValidationSeverity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Async
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.io.StringReader
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Service
class ImportService(
    private val historyService: HistoryService,
    private val conceptRepository: ConceptRepository,
    private val conceptService: ConceptService,
    private val importResultRepository: ImportResultRepository,
    private val objectMapper: ObjectMapper
) {

    @Async("cancel-import-executor")
    fun cancelImport(importId: String) {
        logger.info("Cancelling import with id: $importId")
        updateImportStatus(importId, ImportResultStatus.CANCELLED)
    }

    fun updateImportStatus(importId: String, status: ImportResultStatus) =
        importResultRepository.save(getImportResult(importId).copy(status = status))

    fun confirmImportAndSave(catalogId: String, importId: String, user: User, jwt: Jwt) =
            processAndSaveConcepts(
                catalogId, getImportResult(importId).conceptExtraction ?: emptyList(),
                user, jwt, importId
            )

    private fun getImportResult(importId: String): ImportResult = importResultRepository.findById(importId)
        .orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Import result with id: $importId not found")
        }

    fun updateImportProgress(importId: String, extractedConcepts: Int, totalConcepts: Int) =
            importResultRepository.save(
                getImportResult(importId).copy(
                    extractedConcepts = extractedConcepts,
                    totalConcepts = totalConcepts
                )
            )

    fun updateImportProgress(importId: String, extractedConcepts: Int) =
        importResultRepository.save(
            getImportResult(importId).copy(
                extractedConcepts = extractedConcepts
            )
        )

    fun importRdf(
        catalogId: String, importId: String, concepts: String, lang: Lang, user: User, jwt: Jwt
    ) {
        val model: Model

        try {
            model = ModelFactory.createDefaultModel().apply {
                read(StringReader(concepts), null, lang.name)
            }
        } catch (ex: JenaException) {
            logger.error("Error parsing RDF import", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error during RDF import", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex)
        }

        val conceptsByUri = model.listResourcesWithProperty(RDF.type, SKOS.Concept)
            .asSequence()
            .filter { it.isURIResource }
            .associateBy { it.uri }

        if (conceptsByUri.isEmpty()) {
            logger.warn("No concepts found in RDF import for catalog $catalogId")
            checkIfAlreadyCancelled(importId)
            saveImportResultWithExtractionRecords(catalogId, extractionRecords = emptyList(), ImportResultStatus.FAILED, importId)
        }

        updateImportProgress(
            importId = importId,
            extractedConcepts = 0,
            totalConcepts = conceptsByUri.size
        )

        val conceptExtractions = extractConcepts(conceptsByUri, catalogId, user, importId)

        if (conceptExtractions.isEmpty() || conceptExtractions.hasError) {
            logger.warn("Errors occurred during RDF import for catalog $catalogId")
            checkIfAlreadyCancelled(importId)
            saveImportResultWithExtractionRecords(
                catalogId,
                conceptExtractions.allExtractionRecords,
                ImportResultStatus.FAILED,
                importId
            )
        } else {
            logger.info("Number of concepts extracted: ${conceptExtractions.size} for catalog $catalogId")
            checkIfAlreadyCancelled(importId)
            saveImportResultWithConceptExtractions(
                catalogId = catalogId,
                conceptExtractions = conceptExtractions,
                status = ImportResultStatus.PENDING_CONFIRMATION,
                importId = importId
            )
        }

    }

    fun checkIfAlreadyCancelled(importId: String) {
        val id = importId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Import ID is required")
        val importResult = importResultRepository.findById(id)
            .orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Import result with id: $importId not found")
            }

        if (importResult.status == ImportResultStatus.CANCELLED) {
            logger.warn("Import with id: $importId is already cancelled")
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Import with id: $importId is already cancelled"
            )
        }
    }

    fun createImportResult(catalogId: String): ImportResult {
        val importResult = ImportResult(
            id = UUID.randomUUID().toString(),
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        return importResultRepository.save(importResult)
    }

    fun getResults(catalogId: String): List<ImportResult> {
        return importResultRepository.findAllByCatalogId(catalogId);
    }

    fun getResult(statusId: String): ImportResult? {
        return importResultRepository.findById(statusId).orElse(null)
    }

    fun deleteImportResult(catalogId: String, resultId: String) {
        val result = importResultRepository.findById(resultId).orElse(null)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Import result with id: $resultId not found")

        result.takeIf { result.catalogId == catalogId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Import result with id: $resultId not found in Catalog with id: $catalogId")

        importResultRepository.delete(result)
    }

    private fun extractConcepts(
        conceptsByUri: Map<String, Resource>, catalogId: String, user: User, importId: String? = null
    ): List<ConceptExtraction> {
        val counter = AtomicInteger(0)
        return conceptsByUri.mapNotNull { (uri, resource) ->
            val concept = findLatestConceptByUri(uri) ?: createNewConcept(Virksomhet(id = catalogId), user)
            val conceptExtraction = resource.extract(concept, objectMapper)
            importId?.let {
                checkIfAlreadyCancelled(it)
                updateImportProgress(importId = it, extractedConcepts = counter.incrementAndGet())
            }
            conceptExtraction
        }
    }

    fun saveImportResultWithConceptExtractions(
        catalogId: String,
        conceptExtractions: List<ConceptExtraction>,
        status: ImportResultStatus,
        importId: String? = null
    ): ImportResult = importId
        ?.let { getImportResult(it) }
        ?.let {
            importResultRepository.save(
                it.copy(
                    id = it.id,
                    created = LocalDateTime.now(),
                    catalogId = catalogId,
                    status = status,
                    extractionRecords = conceptExtractions.allExtractionRecords,
                    conceptExtraction = conceptExtractions
                )
            )
        } ?: importResultRepository.save(
            ImportResult(
                id = UUID.randomUUID().toString(),
                created = LocalDateTime.now(),
                catalogId = catalogId,
                status = status,
                extractionRecords = conceptExtractions.allExtractionRecords,
                conceptExtraction = conceptExtractions
            )
    )

    fun saveImportResultWithExtractionRecords(
        catalogId: String, extractionRecords: List<ExtractionRecord>,
        status: ImportResultStatus, importId:String? = null): ImportResult {
        return importResultRepository.save(
            ImportResult(
                id = importId?: UUID.randomUUID().toString(),
                created = LocalDateTime.now(),
                catalogId = catalogId,
                status = status,
                extractionRecords = extractionRecords
            )
        )
    }

    private fun findLatestConceptByUri(uri: String): BegrepDBO? {
        return findExistingConceptId(uri)
            ?.let { conceptRepository.findById(it).orElse(null) }
            ?.let { concept ->
                conceptRepository.getByOriginaltBegrep(concept.originaltBegrep).maxByOrNull { it.versjonsnr }
            }
    }

    private fun findExistingConceptId(externalId: String): String? {
        return importResultRepository.findFirstByStatusAndExtractionRecordsExternalId(
            ImportResultStatus.COMPLETED,
            externalId
        )?.extractionRecords
            ?.firstOrNull { it.externalId == externalId }
            ?.internalId
    }

    private fun processAndSaveConcepts(
        catalogId: String, conceptExtractions: List<ConceptExtraction>, user: User, jwt: Jwt, importId: String? = null
    ): ImportResult {

        val updatedExtractionsHistory = mutableListOf<ExtractionRecord>()
        val savedConceptsDB = mutableListOf<BegrepDBO>();
        val savedConceptsElastic = mutableListOf<BegrepDBO>();
        val concepts = mutableListOf<BegrepDBO>()

        // update history for all concepts and revert all done if any error occurs
        conceptExtractions.forEach { it ->
            val operations = it.extractionRecord.allOperations
            val concept = it.concept
                .updateLastChangedAndByWhom(user)
                .apply { if (erPublisert) createNewRevision() }
            try {
                updateHistory(concept, operations, user, jwt)
                updatedExtractionsHistory.add(it.extractionRecord)
                concepts.add(concept)
            } catch (ex: Exception) {
                logger.error("Failed to update history for concept: ${concept.id}", ex)
                logger.error("Stopping import for all concepts and rolling back all concepts with updated history due to error")
                rollBackUpdates(updatedExtractionsHistory, savedConceptsDB,
                    savedConceptsElastic, jwt)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update history. Import failed", ex)
            }
        }

        // After history is updated safely for all concepts, save them in the DB and update elastic search
        try {
            savedConceptsDB.addAll(saveAllConceptsDB(concepts))
        } catch (ex: Exception) {
            logger.error("Failed to save concepts in DB", ex)
            logger.error("Stopping import for all concepts and rolling back all concepts in updated history and DB due to error")
            rollBackUpdates(updatedExtractionsHistory, savedConceptsDB,
                savedConceptsElastic, jwt)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save imported concepts. Import failed", ex)
        }

        concepts.forEach { concept ->
            try {
                conceptService.updateCurrentConceptForOriginalId(concept.originaltBegrep)
                savedConceptsElastic.add(concept)
            } catch (ex: Exception) {
                logger.error("Failed to save concept in Elastic: ${concept.id}", ex)
                logger.error("Stopping import for all concepts and rolling back all concepts in updated history, DB, and Elastic due to error")
                rollBackUpdates(updatedExtractionsHistory, savedConceptsDB,
                    savedConceptsElastic, jwt)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save imported concepts. Import failed", ex)
            }
        }

        return saveImportResultWithExtractionRecords(catalogId,
            conceptExtractions.map { it.extractionRecord },
            ImportResultStatus.COMPLETED,
            importId)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun saveAllConceptsDB(concepts: List<BegrepDBO>): List<BegrepDBO> {
        return conceptRepository.saveAll(concepts)
    }

    fun rollBackUpdates(updatedExtractionsHistory: List<ExtractionRecord>, savedConceptsDB: List<BegrepDBO>,
                        savedConceptsElastic: List<BegrepDBO>, jwt: Jwt) {
        rollbackHistoryUpdates(updatedExtractionsHistory, jwt)

        try {
            rollBackDbUpdates(savedConceptsDB)
        } catch (ex: Exception) {
            logger.error("Failed to rollback saved concepts from DB", ex)
        }

        try {
            rollBackElasticUpdates(savedConceptsElastic)
        } catch (ex: Exception) {
            logger.error("Failed to rollback saved concepts from Elastic", ex)
        }

    }

    fun rollBackElasticUpdates(savedConceptsElastic: List<BegrepDBO>) {
        savedConceptsElastic.forEach { concept ->
            try {
                conceptService.updateCurrentConceptForOriginalId(concept.originaltBegrep)
            } catch (ex: Exception) {
                logger.error("Failed to rollback elastic updates for concept ${concept.id}", ex)
            }
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun rollBackDbUpdates(savedConceptsDB: List<BegrepDBO>) {
        conceptRepository.deleteAll(savedConceptsDB)
    }

    fun rollbackHistoryUpdates(extractionRecords: List<ExtractionRecord>, jwt: Jwt) {

        extractionRecords.forEach { extractionRecord ->
            val internalId = extractionRecord.internalId

            try {
                historyService.removeHistoryUpdate(internalId, jwt)
                logger.info("Rolled back history updates for concept $internalId was successful")
            } catch (ex: Exception) {
                logger.error("Failed to rollback history updates concept $internalId", ex)
            }
        }

    }

    fun updateHistory(
        concept: BegrepDBO, operations: List<JsonPatchOperation>, user: User, jwt: Jwt,
    ) {
        try {
            historyService.updateHistory(concept, operations, user, jwt)
            logger.info("Updated history for concept: ${concept.id}")
        } catch (ex: Exception) {
            logger.error("Failed to update history for concept: ${concept.id}", ex)

            throw ex
        }
    }
    fun importConcepts(concepts: List<Begrep>, catalogId: String, user: User,
                       jwt: Jwt, importId: String = UUID.randomUUID().toString()): ImportResult {
        conceptService.publishNewCollectionIfFirstSavedConcept(catalogId)

        val begrepUriMap = mutableMapOf<BegrepDBO, String>()
        var extractionRecordMap: Map<BegrepDBO, ExtractionRecord>
        var conceptExtractions: List<ConceptExtraction>

        //Thread.sleep(7000)
        try {
            extractionRecordMap = concepts.map { begrepDTO ->
                checkIfAlreadyCancelled(importId)
                val uuid = UUID.randomUUID().toString()
                val begrepDTOWithUri = findLatestConceptByUri(begrepDTO.id ?: uuid) ?: createNewConcept(
                    begrepDTO.ansvarligVirksomhet,
                    user
                )
                val updatedBegrepDTO = begrepDTOWithUri.updateLastChangedAndByWhom(user)
                val begrepDBO = updatedBegrepDTO.addUpdatableFieldsFromDTO(begrepDTO)
                begrepUriMap[begrepDBO] = begrepDTO.id ?: uuid

                val patchOperations: List<JsonPatchOperation> =
                    createPatchOperations(updatedBegrepDTO, begrepDBO, objectMapper)

                val issues: List<Issue> = extractIssues(begrepDBO, patchOperations)

                val extractionResult = ExtractResult(operations = patchOperations, issues = issues)

                logger.info("Original Begrep ${begrepDBO.originaltBegrep}, anbefalt term: ${begrepDBO.anbefaltTerm}")

                begrepDBO to ExtractionRecord(
                    externalId = begrepUriMap[begrepDBO] ?: begrepDBO?.id ?: uuid,
                    internalId = begrepDBO.id,
                    extractResult = extractionResult
                )
            }.associate {
                checkIfAlreadyCancelled(importId)
                it
            }

            conceptExtractions = extractionRecordMap.map { (concept, record) ->
                checkIfAlreadyCancelled(importId)
                ConceptExtraction(
                    concept = concept,
                    extractionRecord = record
                )
            }

        } catch (responseException: ResponseStatusException) {
            logger.error("Failed to import concepts", responseException)
            throw responseException
        }

        return when {
            conceptExtractions.isEmpty() -> {
                logger.warn("No concepts found in the imported file")
                checkIfAlreadyCancelled(importId)
                saveImportResultWithExtractionRecords(catalogId, extractionRecords = emptyList(),
                    ImportResultStatus.FAILED, importId)
            }
            conceptExtractions.hasError -> {
                checkIfAlreadyCancelled(importId)
                saveImportResultWithExtractionRecords(
                    catalogId, conceptExtractions.allExtractionRecords,
                    ImportResultStatus.FAILED, importId
                )
            }

            else -> {
                checkIfAlreadyCancelled(importId)
                return saveImportResultWithConceptExtractions(
                    catalogId= catalogId,
                    importId = importId,
                    conceptExtractions = conceptExtractions,
                    status = ImportResultStatus.PENDING_CONFIRMATION
                )
            }
        }
    }

    fun importConcepts(concepts: List<Begrep>, catalogId: String, user: User, jwt: Jwt): ImportResult {
        conceptService.publishNewCollectionIfFirstSavedConcept(catalogId)

        val begrepUriMap = mutableMapOf<BegrepDBO, String>()
        val extractionRecordMap: Map<BegrepDBO, ExtractionRecord> = concepts.map { begrepDTO ->
            val uuid = UUID.randomUUID().toString()
            val begrepDTOWithUri = findLatestConceptByUri(begrepDTO.id?: uuid) ?: createNewConcept(begrepDTO.ansvarligVirksomhet, user)
            val updatedBegrepDTO = begrepDTOWithUri.updateLastChangedAndByWhom(user)
            val begrepDBO = updatedBegrepDTO.addUpdatableFieldsFromDTO(begrepDTO)
            begrepUriMap[begrepDBO] = begrepDTO.id?: uuid

            val patchOperations: List<JsonPatchOperation> =
                createPatchOperations(updatedBegrepDTO, begrepDBO, objectMapper)

            val issues: List<Issue> = extractIssues(begrepDBO, patchOperations)

            val extractionResult = ExtractResult(operations = patchOperations, issues = issues)

            logger.info("Original Begrep ${begrepDBO.originaltBegrep}, anbefalt term: ${begrepDBO.anbefaltTerm}")

            begrepDBO to ExtractionRecord(
                externalId = begrepUriMap[begrepDBO] ?: begrepDBO?.id?: uuid,
                internalId = begrepDBO.id,
                extractResult = extractionResult
            )
        }.associate { it }

        val conceptExtractions = extractionRecordMap.map { (concept, record) ->
            ConceptExtraction(
                concept = concept,
                extractionRecord = record
            )
        }

        return when {
            conceptExtractions.isEmpty() -> {
                logger.warn("No concepts found in the imported file")
                saveImportResultWithExtractionRecords(catalogId, emptyList(), ImportResultStatus.FAILED)
            }

            conceptExtractions.hasError -> saveImportResultWithExtractionRecords(catalogId, conceptExtractions.allExtractionRecords, ImportResultStatus.FAILED)

            else -> processAndSaveConcepts(catalogId, conceptExtractions, user, jwt)
        }
    }

    fun extractIssues(begrepDBO: BegrepDBO, patchOerations: List<JsonPatchOperation>): List<Issue> {
        val issues = mutableListOf<Issue>()
        if (patchOerations.isEmpty())
            issues.add(
                Issue(
                    type = IssueType.ERROR,
                    message = "No JsonPatchOperations detected in the concept"
                )
            )

        if (!begrepDBO.validateMinimumVersion()) {
            issues.add(
                Issue(
                    type = IssueType.ERROR,
                    message = "Invalid version ${begrepDBO.versjonsnr}. Version must be minimum 0.1.0"
                )
            )
        }

        val validation = begrepDBO.validateSchema()
        if (!validation.isValid) {
            validation.results().items(ValidationSeverity.ERROR)
                .forEach { result ->
                    issues.add(
                        Issue(
                            type = IssueType.ERROR,
                            message = result.message()
                        )
                    )
                }
        }

        return issues
    }

    fun BegrepDBO.validateMinimumVersion(): Boolean =
        when {
            versjonsnr < SemVer(0, 1, 0) -> false
            else -> true
        }

}

private val logger: Logger = LoggerFactory.getLogger(ImportService::class.java)

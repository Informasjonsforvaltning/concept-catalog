package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.model.BegrepDBO
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

    final val MAX_CONCEPTS_CSV: Int = 500
    final val MAX_CONCEPTS_RDF: Int = 3000
    final val FAILURE_MESSAGE_TOO_MANY_CSV_CONCEPTS =
        "CSV/JSON importen har mer enn $MAX_CONCEPTS_CSV begreper."
    final val FAILURE_MESSAGE_TOO_MANY_RDF_CONCEPTS =
        "RDF importen har mer enn $MAX_CONCEPTS_RDF begreper."
    final val FAILURE_MESSAGE_NO_CONCEPTS = "Fant ingen begreper i importen."

    @Async("cancel-import-executor")
    fun cancelImport(importId: String) {
        logger.info("Cancelling import with id: $importId")
        updateImportStatus(importId, ImportResultStatus.CANCELLED)
        cancelConceptExtractionStatus(importId)
    }

    private fun cancelConceptExtractionStatus(
        importId: String
    ): Unit =
        getImportResult(importId).let {
            val updatedExtractions = it.conceptExtractions.map { conceptExtraction ->
                conceptExtraction.copy(conceptExtractionStatus = ConceptExtractionStatus.CANCELLED)
            }
            importResultRepository.save(it.copy(conceptExtractions = updatedExtractions))
        }

    fun updateImportStatus(importId: String, status: ImportResultStatus, failureMessage: String? = null) =
        getImportResult(importId).let {
            when {
                it.status != status -> importResultRepository.save(
                    it.copy(
                        status = status,
                        failureMessage = failureMessage
                    )
                )
                else -> it
            }
        }

    @Async("import-executor")
    fun confirmImport(importId: String) {
        updateImportStatus(importId = importId, status = ImportResultStatus.SAVING)
    }

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

    fun updateImportSavingProgress(importId: String, savedConcepts: Int) =
        importResultRepository.save(
            getImportResult(importId).copy(
                savedConcepts = savedConcepts
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
            updateImportStatus(
                importId = importId,
                status = ImportResultStatus.FAILED,
                failureMessage = ex.message
            )
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error during RDF import", ex)
            updateImportStatus(
                importId = importId,
                status = ImportResultStatus.FAILED,
                failureMessage = ex.message
            )
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex)
        }

        val conceptsByUri = model.listResourcesWithProperty(RDF.type, SKOS.Concept)
            .asSequence()
            .filter { it.isURIResource }
            .associateBy { it.uri }

        when {
            conceptsByUri.isEmpty() -> {
                logger.warn("No concepts found in RDF import for catalog $catalogId")
                checkIfAlreadyCancelled(importId)
                updateImportStatus(
                    importId = importId,
                    status = ImportResultStatus.FAILED,
                    failureMessage = FAILURE_MESSAGE_NO_CONCEPTS
                )
                return
            }

            conceptsByUri.size > MAX_CONCEPTS_RDF -> {
                logger.warn("Too many concepts found in RDF import for catalog $catalogId")
                checkIfAlreadyCancelled(importId)
                updateImportStatus(
                    importId = importId,
                    status = ImportResultStatus.FAILED,
                    failureMessage = FAILURE_MESSAGE_TOO_MANY_RDF_CONCEPTS
                )
                return
            }

        }

        updateImportProgress(
            importId = importId,
            extractedConcepts = 0,
            totalConcepts = conceptsByUri.size
        )

        val conceptExtractions = extractConcepts(conceptsByUri, catalogId, user, importId)

        when {
            conceptExtractions.isEmpty() ->
                updateImportStatus(
                    importId = importId,
                    status = ImportResultStatus.FAILED,
                    failureMessage = FAILURE_MESSAGE_NO_CONCEPTS
                )

            conceptExtractions.allFailed -> {
                logger.warn("Errors occurred during RDF import for catalog $catalogId")
                checkIfAlreadyCancelled(importId)
                saveImportResultWithConceptExtractions(
                    catalogId = catalogId,
                    conceptExtractions = conceptExtractions,
                    status = ImportResultStatus.FAILED,
                    importId = importId
                )
            }

            else -> {
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
            conceptExtractions = emptyList()
        )
        return importResultRepository.save(importResult)
    }

    fun getResults(catalogId: String): List<ImportResult> {
        logger.info("Getting import results for catalog with id: $catalogId")
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
            val concept = findLatestConceptByUri(uri, catalogId) ?: createNewConcept(Virksomhet(id = catalogId), user)
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
        importId: String? = null,
        failureMessage: String? = null
    ): ImportResult = importId
        ?.let { getImportResult(it) }
        ?.let {
            importResultRepository.save(
                it.copy(
                    id = it.id,
                    created = LocalDateTime.now(),
                    catalogId = catalogId,
                    status = status,
                    conceptExtractions = conceptExtractions
                )
            )
        } ?: importResultRepository.save(
        ImportResult(
            id = UUID.randomUUID().toString(),
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = status,
            conceptExtractions = conceptExtractions
        )
    )

    private fun findLatestConceptByUri(uri: String, catalogId: String): BegrepDBO? {
        return findExistingConceptId(uri, catalogId)
            ?.let { conceptRepository.findById(it).orElse(null) }
            ?.let { concept ->
                conceptRepository.getByOriginaltBegrep(concept.originaltBegrep).maxByOrNull { it.versjonsnr }
            }
    }

    private fun findExistingConceptId(externalId: String, catalogId: String): String? {
        return importResultRepository.findFirstByCatalogIdAndStatusAndConceptExtractionsExtractionRecordExternalId(
            catalogId, ImportResultStatus.COMPLETED, externalId
        )?.conceptExtractions
            ?.allExtractionRecords
            ?.firstOrNull { it.externalId == externalId }
            ?.internalId
    }

    fun updateImportedConceptStatus(importId: String, externalId: String,
                                    conceptExtractionStatus: ConceptExtractionStatus) =
        getImportResult(importId).let { importResult ->
            val updatedExtractions = importResult.conceptExtractions.map {
                if (it.extractionRecord.externalId == externalId)
                    it.copy(conceptExtractionStatus = conceptExtractionStatus)
                else
                    it
            }

            importResultRepository.save(importResult.copy(conceptExtractions = updatedExtractions))
        }


    fun addConceptToCatalog(catalogId: String, importId: String, externalId: String, user: User, jwt: Jwt) {
        logger.info("Adding concept with external ID: $externalId from import with ID: $importId to catalog: $catalogId")

        val importResult = getImportResult(importId)

        val conceptExtraction = importResult.conceptExtractions
            .firstOrNull { it.extractionRecord.externalId == externalId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND,
                "Concept with external ID: $externalId not found in import with ID: $importId")

        conceptExtraction.let {

            val concept = it.concept
            val operations = it.extractionRecord.allOperations

            try {

                updateHistory(concept, operations, user, jwt)
                saveConceptDB(concept)
                conceptService.updateCurrentConceptForOriginalId(concept.originaltBegrep)

            } catch (ex: Exception) {
                logger.error("Failed to add concept ${concept.id} to catalog: ${catalogId}", ex)
                updateImportedConceptStatus(importId, externalId, ConceptExtractionStatus.SAVING_FAILED)
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to add concept to catalog", ex)

            }
            val updatedImportResult = updateImportedConceptStatus(importId, externalId, ConceptExtractionStatus.COMPLETED)
            when {
                updatedImportResult.conceptExtractions.any {
                    it.conceptExtractionStatus == ConceptExtractionStatus.PENDING_CONFIRMATION ||
                            it.conceptExtractionStatus == ConceptExtractionStatus.SAVING_FAILED
                }
                    -> updateImportStatus(importId, ImportResultStatus.PARTIALLY_COMPLETED)

                else -> updateImportStatus(importId, ImportResultStatus.COMPLETED)
            }
            logger.info("Succeeded to add concept with external ID: $externalId from import with ID: $importId to catalog: $catalogId")
        }

    }

    @Transactional(rollbackFor = [Exception::class])
    fun saveAllConceptsDB(concepts: List<BegrepDBO>): List<BegrepDBO> {
        return conceptRepository.saveAll(concepts)
    }

    fun saveConceptDB(concept: BegrepDBO): BegrepDBO {
        return conceptRepository.save(concept)
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

        if (concepts.size > MAX_CONCEPTS_CSV)
            return updateImportStatus(
                importId = importId,
                status = ImportResultStatus.FAILED,
                failureMessage= FAILURE_MESSAGE_TOO_MANY_CSV_CONCEPTS
            )

        conceptService.publishNewCollectionIfFirstSavedConcept(catalogId)

        val begrepUriMap = mutableMapOf<BegrepDBO, String>()
        var extractionRecordMap: Map<BegrepDBO, ExtractionRecord>
        var conceptExtractions: List<ConceptExtraction>

        updateImportProgress(
            importId = importId,
            extractedConcepts = 0,
            totalConcepts = concepts.size
        )

        try {
            val counter = AtomicInteger(0)
            extractionRecordMap = concepts.map { begrepDTO ->
                checkIfAlreadyCancelled(importId)
                val uuid = UUID.randomUUID().toString()
                val begrepDTOWithUri = findLatestConceptByUri(begrepDTO.id ?: uuid, catalogId) ?: createNewConcept(
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

                updateImportProgress(importId = importId, extractedConcepts = counter.incrementAndGet())

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
                saveImportResultWithConceptExtractions(catalogId = catalogId,
                    conceptExtractions = emptyList(),
                    status =ImportResultStatus.FAILED, importId = importId,
                    failureMessage = FAILURE_MESSAGE_NO_CONCEPTS)
            }
            conceptExtractions.allFailed -> {
                checkIfAlreadyCancelled(importId)
                saveImportResultWithConceptExtractions(
                    catalogId = catalogId, conceptExtractions = conceptExtractions,
                    status = ImportResultStatus.FAILED, importId = importId
                )
            }

            else -> {
                checkIfAlreadyCancelled(importId)
                try {
                    return saveImportResultWithConceptExtractions(
                        catalogId= catalogId,
                        importId = importId,
                        conceptExtractions = conceptExtractions,
                        status = ImportResultStatus.PENDING_CONFIRMATION
                    )
                } catch (exception: Exception) {
                    logger.error("Failed to finalize importing concepts", exception)
                    updateImportStatus(
                        importId = importId,
                        status = ImportResultStatus.FAILED,
                        failureMessage = exception.message
                        )
                    throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to finalize importing concepts", exception)
                }
            }
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

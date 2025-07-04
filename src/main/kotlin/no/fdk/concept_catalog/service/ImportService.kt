package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.model.*
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
import org.openapi4j.core.validation.ValidationResults
import org.openapi4j.core.validation.ValidationSeverity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.StringReader
import java.time.LocalDateTime
import java.util.*

@Service
class ImportService(
    private val historyService: HistoryService,
    private val conceptRepository: ConceptRepository,
    private val conceptService: ConceptService,
    private val importResultRepository: ImportResultRepository,
    private val objectMapper: ObjectMapper
) {
    fun importRdf(catalogId: String, concepts: String, lang: Lang, user: User, jwt: Jwt): ImportResult {
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
            return saveImportResult(catalogId, emptyList(), ImportResultStatus.FAILED)
        }

        val conceptExtractions = extractConcepts(conceptsByUri, catalogId, user)

        return if (conceptExtractions.isEmpty() || conceptExtractions.hasError) {
            logger.warn("Errors occurred during RDF import for catalog $catalogId")
            saveImportResult(catalogId, conceptExtractions.allExtractionRecords, ImportResultStatus.FAILED)
        } else {
            processAndSaveConcepts(catalogId, conceptExtractions, user, jwt)
        }
    }

    fun getResults(catalogId: String): List<ImportResult> {
        return importResultRepository.findAllByCatalogId(catalogId);
    }

    fun getResult(statusId: String): ImportResult? {
        return importResultRepository.findByIdOrNull(statusId)
    }

    fun deleteImportResult(catalogId: String, resultId: String) {
        val result = importResultRepository.findByIdOrNull(resultId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Import result with id: $resultId not found")

        result.takeIf { result.catalogId == catalogId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Import result with id: $resultId not found in Catalog with id: $catalogId")

        importResultRepository.delete(result)
    }

    private fun extractConcepts(
        conceptsByUri: Map<String, Resource>, catalogId: String, user: User
    ): List<ConceptExtraction> {
        return conceptsByUri.mapNotNull { (uri, resource) ->
            val concept = findLatestConceptByUri(uri) ?: createNewConcept(Virksomhet(id = catalogId), user)

            resource.extract(concept, objectMapper)
        }
    }

    private fun saveImportResult(
        catalogId: String, extractionRecords: List<ExtractionRecord>, status: ImportResultStatus
    ): ImportResult {
        return importResultRepository.save(
            ImportResult(
                id = UUID.randomUUID().toString(),
                created = LocalDateTime.now(),
                catalogId = catalogId,
                status = status,
                extractionRecords = extractionRecords
            )
        )
    }

    private fun processAndSaveConcepts(
        catalogId: String, conceptExtractions: List<ConceptExtraction>, user: User, jwt: Jwt
    ): ImportResult {
        val processedRecords = mutableListOf<ExtractionRecord>()

        try {
            for (extraction in conceptExtractions) {
                val updatedRecord = updateConcept(catalogId, extraction, user, jwt)
                processedRecords.add(updatedRecord)
            }
        } catch (ex: Exception) {
            logger.error("Error during RDF processing. Rolling back all processed concepts.", ex)
            rollbackProcessedConcepts(processedRecords, jwt)

            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error. Changes rolled back.",
                ex
            )
        }

        return saveImportResult(catalogId, processedRecords, ImportResultStatus.COMPLETED)
    }

    private fun rollbackProcessedConcepts(extractionRecords: List<ExtractionRecord>, jwt: Jwt) {
        for (extractionRecord in extractionRecords) {
            val internalId = extractionRecord.internalId

            try {
                conceptRepository.deleteById(internalId)
                historyService.removeHistoryUpdate(internalId, jwt)

                logger.info("Rolled back concept $internalId successfully")
            } catch (ex: Exception) {
                logger.error("Failed to rollback concept $internalId", ex)
            }
        }
    }

    private fun findLatestConceptByUri(uri: String): BegrepDBO? {
        return findExistingConceptId(uri)
            ?.let { conceptRepository.findByIdOrNull(it) }
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

    private fun updateConcept(
        catalogId: String, conceptExtraction: ConceptExtraction, user: User, jwt: Jwt
    ): ExtractionRecord {
        val operations = conceptExtraction.extractionRecord.allOperations

        val concept = conceptExtraction.concept
            .updateLastChangedAndByWhom(user)
            .apply { if (erPublisert) createNewRevision() }

        val savedConcept = conceptRepository.save(concept)
        logger.info("Updated concept in catalog $catalogId by user ${user.id}: ${savedConcept.id}")

        conceptService.updateCurrentConceptForOriginalId(savedConcept.originaltBegrep)
        logger.info("Updated ElasticSearch for concept: ${savedConcept.id} in catalog $catalogId by user ${user.id}")

        updateHistory(savedConcept, operations, user, jwt)

        return conceptExtraction.extractionRecord
    }

    private fun updateHistory(
        concept: BegrepDBO, operations: List<JsonPatchOperation>, user: User, jwt: Jwt,
    ) {
        try {
            historyService.updateHistory(concept, operations, user, jwt)
            logger.info("Updated history for concept: ${concept.id}")
        } catch (ex: Exception) {
            logger.error("Failed to update history for concept: ${concept.id}", ex)
            conceptRepository.deleteById(concept.id)

            throw ex
        }
    }

    fun importConcepts(concepts: List<Begrep>, catalogId: String, user: User, jwt: Jwt): ImportResult {
        concepts.map { it.ansvarligVirksomhet.id }
            .distinct()
            .forEach { conceptService.publishNewCollectionIfFirstSavedConcept(it) }

        val extractionRecordMap = mutableMapOf<BegrepDBO, ExtractionRecord>()
        val begrepUriMap = mutableMapOf<BegrepDBO, String>();
        val newConceptsAndOperations = concepts
            .map {
                it to (
                        findLatestConceptByUri(it.uri!!) ?: createNewConcept(it.ansvarligVirksomhet, user)
                ).updateLastChangedAndByWhom(user)
            }
            .associate {
                val begrepDBO = it.second.addUpdatableFieldsFromDTO(it.first) to it.second
                begrepUriMap[begrepDBO.first] = it?.first?.uri!!
                begrepDBO
            }
            .mapValues { createPatchOperations(it.value, it.key, objectMapper) }
            .onEach {
                logger.info("Original Begrep ${it.key.originaltBegrep}, anbefalt term: ${it.key.anbefaltTerm}")
                it.value.forEach { patch -> logger.info("Operations ${patch}") }
                val issues: List<Issue> = extractIssues(it.key, it.value)

                extractionRecordMap[it.key] = ExtractionRecord(
                    externalId = begrepUriMap[it.key] ?: it.key.id,
                    internalId = it.key.id,
                    extractResult = ExtractResult(
                        operations = it.value,
                        issues = issues
                    )
                )

            }

        val conceptExtractions = extractionRecordMap.map { (concept, record) ->
            ConceptExtraction(
                concept = concept,
                extractionRecord = record
            )
        }

        return when {
            conceptExtractions.isEmpty() -> {
                logger.warn("No concepts found in the imported file")
                saveImportResult(catalogId, emptyList(), ImportResultStatus.FAILED)
            }
            conceptExtractions.hasError -> saveImportResult(catalogId, conceptExtractions.allExtractionRecords, ImportResultStatus.FAILED)

            else -> {
                conceptService.saveConceptsAndUpdateHistory(newConceptsAndOperations, user, jwt)
                    .takeIf { it.isNotEmpty() }
                    ?.also {
                        logger.debug("created ${it.size} new concepts for ${it.first().ansvarligVirksomhet.id}")
                    }
                saveImportResult(catalogId, conceptExtractions.allExtractionRecords, ImportResultStatus.COMPLETED)
            }
        }
    }

    private fun extractIssues(begrepDBO: BegrepDBO, patchOerations: List<JsonPatchOperation>): List<Issue> {
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
        validation.results().items(ValidationSeverity.WARNING)
            .forEach { validation ->
                issues.add(
                    Issue(
                        type = IssueType.WARNING,
                        message = validation.message()
                    )
                )
            }
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

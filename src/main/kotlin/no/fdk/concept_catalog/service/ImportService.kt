package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.rdf.extract
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.repository.ImportResultRepository
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.shared.JenaException
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
    private val importResultRepository: ImportResultRepository,
    private val objectMapper: ObjectMapper
) {

    fun importRdf(catalogId: String, concepts: String, lang: Lang, user: User, jwt: Jwt): ImportResult {
        val model = ModelFactory.createDefaultModel()

        try {
            model.read(StringReader(concepts), "https://example.com", Lang.TURTLE.name)
        } catch (ex: JenaException) {
            logger.error("Error parsing RDF import: ${ex.message}", ex)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message, ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error during RDF import", ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex)
        }

        val extractionRecords = model.extract()

        if (extractionRecords.isEmpty()) {
            logger.warn("No concepts extracted from RDF import for catalog $catalogId")
            return saveImportResult(catalogId, emptyList(), ImportResultStatus.FAILED)
        }

        if (extractionRecords.hasError) {
            logger.warn("RDF import extraction errors occurred for catalog $catalogId")
            return saveImportResult(catalogId, extractionRecords, ImportResultStatus.FAILED)
        }

        val processedRecords = mutableListOf<ExtractionRecord>()

        try {
            extractionRecords.forEach { extractionRecord ->
                val existingConceptId = findExistingConceptId(extractionRecord.externalId)

                val record = if (existingConceptId != null) {
                    replaceExistingConcept(catalogId, existingConceptId, extractionRecord, user, jwt)
                } else {
                    createNewConcept(catalogId, extractionRecord, user, jwt)
                }

                processedRecords.add(record)
            }
        } catch (ex: Exception) {
            logger.error("Unexpected error during RDF processing. Rolling back changes...", ex)

            processedRecords.forEach { record ->
                val internalId = record.internalId ?: return@forEach

                try {
                    conceptRepository.deleteById(internalId)
                    historyService.removeHistoryUpdate(record.internalId, jwt)
                    logger.info("Rolled back concept $internalId successfully")
                } catch (deleteEx: Exception) {
                    logger.error("Failed to delete concept $internalId during rollback", deleteEx)
                }
            }

            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", ex)
        }

        return saveImportResult(catalogId, processedRecords, ImportResultStatus.COMPLETED)
    }

    fun getResults(catalogId: String): List<ImportResult> {
        return importResultRepository.findAllByCatalogId(catalogId);
    }

    fun getResult(statusId: String): ImportResult? {
        return importResultRepository.findByIdOrNull(statusId)
    }

    private fun saveImportResult(
        catalogId: String,
        extractionRecords: List<ExtractionRecord>,
        status: ImportResultStatus
    ): ImportResult {
        val importResult = ImportResult(
            id = UUID.randomUUID().toString(),
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = status,
            extractionRecords = extractionRecords
        )

        return importResultRepository.save(importResult)
    }

    private fun findExistingConceptId(externalId: String): String? {
        return importResultRepository.findFirstByStatusAndExtractionRecordsExternalId(
            ImportResultStatus.COMPLETED,
            externalId
        )?.extractionRecords
            ?.firstOrNull { it.externalId == externalId }
            ?.internalId
    }

    private fun replaceExistingConcept(
        catalogId: String,
        existingConceptId: String,
        extractionRecord: ExtractionRecord,
        user: User,
        jwt: Jwt
    ): ExtractionRecord {
        val existingConcept = conceptRepository.findByIdOrNull(existingConceptId)
            ?.let { conceptRepository.getByOriginaltBegrep(it.originaltBegrep) }
            ?.maxByOrNull { it.versjonsnr }

        val updatedConcept: BegrepDBO = when {
            existingConcept == null -> createNewConcept(Virksomhet(id = catalogId), user)
            existingConcept.erPublisert -> existingConcept
            else -> existingConcept.createNewRevision()
        }

        val patchedConcept =
            patchOriginal(updatedConcept.copy(endringslogelement = null), extractionRecord.allOperations, objectMapper)
                .copy(
                    id = updatedConcept.id,
                    originaltBegrep = updatedConcept.originaltBegrep,
                    ansvarligVirksomhet = updatedConcept.ansvarligVirksomhet
                ).updateLastChangedAndByWhom(user)

        val savedConcept = conceptRepository.save(patchedConcept)
        logger.info("Updated concept in catalog $catalogId by user ${user.id}: ${savedConcept.id}")

        historyService.updateHistory(savedConcept, extractionRecord.allOperations, user, jwt)
        logger.info("Updated history in catalog $catalogId by user ${user.id}: ${savedConcept.id}")

        return extractionRecord.copy(internalId = savedConcept.id)
    }

    private fun createNewConcept(
        catalogId: String,
        extractionRecord: ExtractionRecord,
        user: User,
        jwt: Jwt
    ): ExtractionRecord {
        val newConcept = createNewConcept(Virksomhet(id = catalogId), user)

        val patchedConcept = patchOriginal(
            original = newConcept,
            operations = extractionRecord.allOperations,
            mapper = objectMapper
        ).updateLastChangedAndByWhom(user)

        val savedConcept = conceptRepository.save(patchedConcept)
        logger.info("Created concept in catalog $catalogId by user ${user.id}: ${savedConcept.id}")

        historyService.updateHistory(savedConcept, extractionRecord.allOperations, user, jwt)
        logger.info("Updated history in catalog $catalogId by user ${user.id}: ${savedConcept.id}")

        return extractionRecord.copy(internalId = savedConcept.id)
    }
}

private val logger: Logger = LoggerFactory.getLogger(ImportService::class.java)

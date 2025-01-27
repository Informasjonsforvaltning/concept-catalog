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
import java.util.*

@Service
class ImportService(
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

        val processedExtractionRecords = extractionRecords.map { extractionRecord ->
            val existingConceptId = findExistingConceptId(extractionRecord.externalId)

            if (existingConceptId != null) {
                replaceExistingConcept(catalogId, existingConceptId, extractionRecord, user)
            } else {
                createNewConcept(catalogId, extractionRecord, user)
            }
        }

        return saveImportResult(catalogId, processedExtractionRecords, ImportResultStatus.COMPLETED)
    }

    fun getStatus(statusId: String): ImportResult? {
        return importResultRepository.findByIdOrNull(statusId)
    }

    private fun saveImportResult(
        catalogId: String,
        extractionRecords: List<ExtractionRecord>,
        status: ImportResultStatus
    ): ImportResult {
        val importResult = ImportResult(
            id = UUID.randomUUID().toString(),
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
        user: User
    ): ExtractionRecord {
        conceptRepository.deleteByOriginaltBegrep(existingConceptId)

        val concept = createNewConcept(Virksomhet(id = catalogId), user)
            .copy(id = existingConceptId, originaltBegrep = existingConceptId)
            .updateLastChangedAndByWhom(user)

        val updatedConcept = patchOriginal(
            original = concept,
            operations = extractionRecord.extractResult.operations.toList(),
            mapper = objectMapper
        )

        conceptRepository.save(updatedConcept)

        logger.info("Concept replaced in catalog $catalogId by user ${user.id}: $existingConceptId → ${updatedConcept.originaltBegrep}")

        return extractionRecord.copy(internalId = updatedConcept.originaltBegrep)
    }

    private fun createNewConcept(catalogId: String, extractionRecord: ExtractionRecord, user: User): ExtractionRecord {
        val concept = createNewConcept(Virksomhet(id = catalogId), user)
            .updateLastChangedAndByWhom(user)

        val updatedConcept = patchOriginal(
            original = concept,
            operations = extractionRecord.extractResult.operations.toList(),
            mapper = objectMapper
        )

        conceptRepository.save(updatedConcept)

        logger.info("New concept created in catalog $catalogId by user ${user.id}: ${updatedConcept.originaltBegrep}")

        return extractionRecord.copy(internalId = updatedConcept.originaltBegrep)
    }
}

private val logger: Logger = LoggerFactory.getLogger(ImportService::class.java)

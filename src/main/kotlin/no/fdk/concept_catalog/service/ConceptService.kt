package no.fdk.concept_catalog.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.json.Json
import jakarta.json.JsonException
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.validation.isValid
import no.fdk.concept_catalog.validation.validateSchema
import org.openapi4j.core.validation.ValidationResults
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.io.StringReader

private val logger = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val conceptRepository: ConceptRepository,
    private val mongoOperations: MongoOperations,
    private val applicationProperties: ApplicationProperties,
    private val conceptPublisher: ConceptPublisher,
    private val mapper: ObjectMapper
) {

    fun deleteConcept(concept: BegrepDBO) =
        conceptRepository.delete(concept)

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)?.withHighestVersionDTO()

    fun getConceptDBO(id: String): BegrepDBO? =
        conceptRepository.findByIdOrNull(id)

    fun createConcept(concept: Begrep, userId: String): Begrep {
        val newConcept: BegrepDBO = concept.mapForCreation()
                .also { publishNewCollectionIfFirstSavedConcept(concept.ansvarligVirksomhet?.id) }
                .updateLastChangedAndByWhom(userId)

        val validation = newConcept.validateSchema()
        if (!validation.isValid) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, validation.results().toString())
        }

        return conceptRepository.save(newConcept).withHighestVersionDTO()
    }

    fun getAllCollections(): List<Begrepssamling> =
        getAllPublisherIds()
            .map { getCollectionForPublisher(it) }

    fun getCollectionsForOrganizations(publishers: Set<String>): List<Begrepssamling> =
        publishers
            .map { getCollectionForPublisher(it) }
            .filter { it.antallBegrep > 0 }

    private fun getCollectionForPublisher(publisherId: String): Begrepssamling =
        Begrepssamling(
            id = publisherId,
            antallBegrep = getConceptsForOrganization(publisherId, null)
                .distinctBy { it.originaltBegrep }
                .size
        )

    fun createRevisionOfConcept(revisionValues: Begrep, concept: BegrepDBO, userId: String): Begrep =
        concept.let { revisionValues.createRevision(it) }
            .updateLastChangedAndByWhom(userId)
            .let { conceptRepository.save(it) }
            .withHighestVersionDTO()

    fun createConcepts(concepts: List<Begrep>, userId: String) {
        concepts.mapNotNull { it.ansvarligVirksomhet?.id }
            .distinct()
            .forEach { publishNewCollectionIfFirstSavedConcept(it) }

        val validationResultsMap = mutableMapOf<BegrepDBO, ValidationResults>()
        val newConcepts = concepts
            .map { it.mapForCreation().updateLastChangedAndByWhom(userId) }
            .onEach {
            val validation = it.validateSchema()
            if (!validation.isValid) {
                validationResultsMap[it] = validation.results()
            }
        }

        if (validationResultsMap.isNotEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                validationResultsMap.entries.mapIndexed { index, entry ->
                    "Begrep ${index}"
                        .plus(entry.key.anbefaltTerm?.navn?.let { " - $it" } ?: "")
                        .plus("\n")
                        .plus(entry.value.toString())
                        .plus("\n\n")
                }.joinToString("\n")
            )
        }

        conceptRepository.saveAll(newConcepts)
    }

    fun updateConcept(concept: BegrepDBO, operations: List<JsonPatchOperation>, userId: String): Begrep {
        val patched = try {
            patchBegrep(
                concept.copy(endringslogelement = null),
                operations
            )
                .copy(
                    id = concept.id,
                    originaltBegrep = concept.originaltBegrep,
                    ansvarligVirksomhet = concept.ansvarligVirksomhet
                )
                .updateLastChangedAndByWhom(userId)
        } catch (ex: Exception) {
            logger.error("PATCH failed for ${concept.id}", ex)
            when (ex) {
                is JsonException -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
                is JsonProcessingException -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
                is IllegalArgumentException -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
                else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.message)
            }
        }

        val validation = patched.validateSchema()

        when {
            concept.status == Status.PUBLISERT -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unable to patch published concepts"
            )
            !validation.isValid -> throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                validation.results().toString()
            )
            isNonDraftAndNotValid(patched.withHighestVersionDTO()) -> {
                val badRequestException = ResponseStatusException(HttpStatus.BAD_REQUEST)
                logger.error(
                    "Concept ${patched.id} has not passed validation for non draft concepts and has not been saved.",
                    badRequestException
                )
                throw badRequestException
            }
            patched.status == Status.PUBLISERT -> concept.ansvarligVirksomhet?.id?.let { publisherId ->
                conceptPublisher.send(publisherId)
            }
        }
        return conceptRepository.save(patched).withHighestVersionDTO()
    }

    fun isNonDraftAndNotValid(concept: Begrep): Boolean {
        val published = getLastPublished(concept.originaltBegrep)
        return when {
            concept.status == Status.UTKAST -> false
            concept.versjonsnr == null -> true
            !concept.isValid() -> true
            published?.versjonsnr == null -> false
            else -> published.versjonsnr >= concept.versjonsnr
        }
    }

    fun getConceptsForOrganization(orgNr: String, status: Status?): List<Begrep> =
        if (status == null) conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr).map { it.withHighestVersionDTO() }
        else conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr, status).map { it.withHighestVersionDTO() }

    fun statusFromString(str: String?): Status? =
        when (str?.lowercase()) {
            Status.UTKAST.value -> Status.UTKAST
            Status.GODKJENT.value -> Status.GODKJENT
            Status.HOERING.value -> Status.HOERING
            Status.PUBLISERT.value -> Status.PUBLISERT
            else -> null
        }

    fun getAllPublisherIds(): List<String> {
        return mongoOperations
            .query(Begrep::class.java)
            .distinct("ansvarligVirksomhet.id")
            .`as`(String::class.java)
            .all()
    }

    fun getLastPublished(originaltBegrep: String?): Begrep? =
        if (originaltBegrep == null) null
        else {
            conceptRepository.getByOriginaltBegrepAndStatus(originaltBegrep, Status.PUBLISERT)
                .maxByOrNull { concept -> concept.versjonsnr }
                ?.let { it.toDTO(it.versjonsnr, it.id) }
        }

    fun getLastPublishedForOrganization(orgNr: String): List<Begrep> =
        conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr, Status.PUBLISERT)
            .sortedByDescending {concept -> concept.versjonsnr }
            .distinctBy {concept -> concept.originaltBegrep }
            .map { it.toDTO(it.versjonsnr, it.id) }

    fun searchConceptsByTerm(orgNumber: String, query: String): List<Begrep> =
        conceptRepository.findByTermLike(orgNumber, query).map { it.withHighestVersionDTO() }.toList()

    private fun publishNewCollectionIfFirstSavedConcept(publisherId: String?) {
        val begrepCount = publisherId?.let {
            conceptRepository.countBegrepByAnsvarligVirksomhetId(it)
        }

        if (begrepCount == 0L) {
            logger.info("Adding first entry for $publisherId in harvest admin...")
            val harvestUrl = UriComponentsBuilder
                .fromUriString(applicationProperties.collectionBaseUri)
                .replacePath("/collections/$publisherId")
                .build().toUriString()
            conceptPublisher.sendNewDataSource(publisherId, harvestUrl)
        }
    }

    private fun patchBegrep(begrep: BegrepDBO, operations: List<JsonPatchOperation>): BegrepDBO {
        if (operations.isNotEmpty()) {
            with(mapper) {
                val changes = Json.createReader(StringReader(writeValueAsString(operations))).readArray()
                val original = Json.createReader(StringReader(writeValueAsString(begrep))).readObject()

                return Json.createPatch(changes).apply(original)
                    .let { readValue(it.toString()) }
            }
        }
        return begrep
    }

    private fun BegrepDBO.withHighestVersionDTO(): Begrep =
        getLastPublished(originaltBegrep)
            ?.let { toDTO(it.versjonsnr, it.id) }
            ?: toDTO(null, null)

}

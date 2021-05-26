package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Endringslogelement
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.validation.isValid
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.StringReader
import java.time.LocalDateTime
import javax.json.Json
import javax.json.JsonException

private val logger = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val conceptRepository: ConceptRepository,
    private val mongoOperations: MongoOperations
) {

    fun deleteConcept(concept: Begrep) =
        conceptRepository.delete(concept)

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)

    fun createConcept(concept: Begrep, userId: String): Begrep =
        concept.copy(id = null, status = Status.UTKAST)
            .updateLastChangedAndByWhom(userId)
            .let { conceptRepository.save(concept) }

    fun createConcepts(concepts: List<Begrep>, userId: String) {
        conceptRepository.saveAll(
            concepts.map {
                it.copy(id = null, status = Status.UTKAST)
                    .updateLastChangedAndByWhom(userId)
            }
        )
    }

    fun updateConcept(concept: Begrep, operations: List<JsonPatchOperation>, userId: String): Begrep {
        val patched = try {
            patchBegrep(concept.copy(endringslogelement = null), operations)
                .copy(
                    id = concept.id,
                    ansvarligVirksomhet = concept.ansvarligVirksomhet)
                .updateLastChangedAndByWhom(userId)
        } catch (jsonException: JsonException) {
            logger.error("PATCH failed for ${concept.id}", jsonException)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

        if (patched.status != Status.UTKAST && !patched.isValid()) {
            logger.error("Concept ${patched.id} has not passed validation for non draft concepts and has not been saved.")
            throw ResponseStatusException(HttpStatus.CONFLICT)
        }

        return conceptRepository.save(patched)
    }

    fun getConceptsForOrganization(orgNr: String, status: Status?): List<Begrep> =
        if (status == null) conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr)
        else conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr, status)

    fun statusFromString(str: String?): Status? =
        when(str?.lowercase()) {
            Status.UTKAST.value -> Status.UTKAST
            Status.GODKJENT.value -> Status.GODKJENT
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

}

private fun patchBegrep(begrep: Begrep, operations: List<JsonPatchOperation>): Begrep {
    if (operations.isNotEmpty()) {
        with(ObjectMapper().registerModule(JavaTimeModule())) {
            val changes = Json.createReader(StringReader(writeValueAsString(operations))).readArray()
            val original = Json.createReader(StringReader(writeValueAsString(begrep))).readObject()

            return Json.createPatch(changes).apply(original)
                .let { readValue(it.toString(), Begrep::class.java) }
        }
    }
    return begrep
}

private fun Begrep.updateLastChangedAndByWhom(userId: String): Begrep =
    copy(
        endringslogelement = Endringslogelement(
            endringstidspunkt = LocalDateTime.now(),
            brukerId = userId
        )
    )

package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.model.ChangeRequestForCreate
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.repository.ChangeRequestRepository
import no.fdk.concept_catalog.repository.ConceptRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class ChangeRequestService(
    private val changeRequestRepository: ChangeRequestRepository,
    private val conceptRepository: ConceptRepository,
    private val mapper: ObjectMapper
) {
    fun getAllCatalogRequests(catalogId: String): List<ChangeRequest> =
        changeRequestRepository.getByCatalogId(catalogId)

    fun getByConceptId(catalogId: String, conceptId: String): List<ChangeRequest> =
        changeRequestRepository.getByCatalogIdAndConceptId(catalogId, conceptId)

    fun deleteChangeRequest(id: String, catalogId: String): Unit =
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.let { toDelete -> changeRequestRepository.delete(toDelete) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    fun createChangeRequest(toCreate: ChangeRequestForCreate, catalogId: String): String {
        if (toCreate.conceptId != null) {
            validateConceptAsPartOfCatalog(toCreate.conceptId, catalogId)
        }
        val newId = UUID.randomUUID().toString()
        ChangeRequest(
            id = newId,
            catalogId = catalogId,
            conceptId = toCreate.conceptId,
            anbefaltTerm = toCreate.anbefaltTerm,
            tillattTerm = toCreate.tillattTerm,
            frarådetTerm = toCreate.frarådetTerm,
            definisjon = toCreate.definisjon
        ).run { changeRequestRepository.save(this) }

        return newId
    }

    fun updateChangeRequest(id: String, catalogId: String, operations: List<JsonPatchOperation>):ChangeRequest? {
        validateJsonPatchOperations(operations)
        return changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.let { patchOriginal(it, operations, mapper) }
            ?.let { changeRequestRepository.save(it) }
    }

    fun getByIdAndCatalogId(id: String, catalogId: String): ChangeRequest? =
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)

    private fun validateConceptAsPartOfCatalog(conceptId: String, catalogId: String){
        val concept = conceptRepository.findByIdOrNull(conceptId)
        if (concept?.ansvarligVirksomhet?.id != catalogId || concept.originaltBegrep != conceptId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }
    }

    private fun validateJsonPatchOperations(operations: List<JsonPatchOperation>) {
        val invalidPaths = listOf("/id", "/catalogId", "/conceptId")
        if (operations.any { it.path in invalidPaths }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patch of paths $invalidPaths is not permitted")
        }
    }
}

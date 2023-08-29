package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.model.*
import java.util.UUID
import no.fdk.concept_catalog.repository.ChangeRequestRepository
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.validation.isOrganizationNumber
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

private val logger = LoggerFactory.getLogger(ChangeRequestService::class.java)

@Service
class ChangeRequestService(
    private val changeRequestRepository: ChangeRequestRepository,
    private val conceptRepository: ConceptRepository,
    private val conceptService: ConceptService,
    private val mapper: ObjectMapper
) {
    fun getCatalogRequests(catalogId: String, status: String?, conceptId: String?): List<ChangeRequest>  {
        val parsedStatus = changeRequestStatusFromString(status)
        return when {
            parsedStatus != null && conceptId != null -> changeRequestRepository.getByCatalogIdAndStatusAndConceptId(catalogId, parsedStatus, conceptId)
            parsedStatus != null -> changeRequestRepository.getByCatalogIdAndStatus(catalogId, parsedStatus)
            conceptId != null -> changeRequestRepository.getByCatalogIdAndConceptId(catalogId, conceptId)
            else -> changeRequestRepository.getByCatalogId(catalogId)
        }
    }

    fun deleteChangeRequest(id: String, catalogId: String): Unit =
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.let { toDelete -> changeRequestRepository.delete(toDelete) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    fun createChangeRequest(catalogId: String, user: User, body: ChangeRequestUpdateBody): String {
        validateNewChangeRequest(body.conceptId, catalogId)
        val newId = UUID.randomUUID().toString()
        ChangeRequest(
            id = newId,
            catalogId = catalogId,
            conceptId = body.conceptId,
            status = ChangeRequestStatus.OPEN,
            operations = emptyList(),
            proposedBy = user,
            timeForProposal = Instant.now(),
            title = body.title,
        ).run { changeRequestRepository.save(this) }

        return newId
    }

    fun updateChangeRequest(id: String, catalogId: String, body: ChangeRequestUpdateBody): ChangeRequest? {
        validateJsonPatchOperations(body.operations)
        return changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.copy(
                operations = body.operations,
                title = body.title
            )
            ?.let { changeRequestRepository.save(it) }
    }

    fun acceptChangeRequest(id: String, catalogId: String, user: User, jwt: Jwt): String {
        val changeRequest = changeRequestRepository.getByIdAndCatalogId(id, catalogId)

        changeRequest?.also { if (it.status != ChangeRequestStatus.OPEN) throw ResponseStatusException(HttpStatus.BAD_REQUEST) }
            ?.copy(status = ChangeRequestStatus.ACCEPTED)
            ?.run { changeRequestRepository.save(this) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        val dbConcept = changeRequest.conceptId
            ?.let { conceptRepository.getByOriginaltBegrep(it) }
            ?.maxByOrNull { it.versjonsnr }

        val conceptToUpdate = when {
            dbConcept == null -> conceptRepository.save(
                createNewConcept(Virksomhet(id=catalogId), user)
                    .updateLastChangedAndByWhom(user)
            )
            dbConcept.erPublisert -> conceptRepository.save(
                dbConcept.createNewRevision(user)
                    .updateLastChangedAndByWhom(user)
            )
            else -> dbConcept
        }

        try {
            conceptService.updateConcept(conceptToUpdate, changeRequest.operations, user, jwt)
        } catch (ex: Exception) {
            logger.error("update of concept failed when accepting ${changeRequest.id}, reverting acceptation", ex)
            changeRequest.copy(status = ChangeRequestStatus.OPEN).run { changeRequestRepository.save(this) }
            if (conceptToUpdate.id != dbConcept?.id) conceptRepository.delete(conceptToUpdate)
            throw ex
        }

        return conceptToUpdate.id
    }

    fun rejectChangeRequest(id: String, catalogId: String) {
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.also { if (it.status != ChangeRequestStatus.OPEN) throw ResponseStatusException(HttpStatus.BAD_REQUEST) }
            ?.copy(status = ChangeRequestStatus.REJECTED)
            ?.run { changeRequestRepository.save(this) }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    fun getByIdAndCatalogId(id: String, catalogId: String): ChangeRequest? =
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)

    private fun validateNewChangeRequest(conceptId: String?, catalogId: String) {
        if (!catalogId.isOrganizationNumber()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided catalogId is not valid organization number")
        } else if (conceptId != null) {
            val openChangeRequestForConcept = changeRequestRepository.getByConceptIdAndStatus(conceptId, ChangeRequestStatus.OPEN)
            val concept = conceptRepository.findByIdOrNull(conceptId)

            when {
                openChangeRequestForConcept.isNotEmpty() -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Found unhandled change request for concept")
                concept?.ansvarligVirksomhet?.id != catalogId -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Concept is part of another collection")
                concept.originaltBegrep != conceptId -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided conceptId is not the original")
            }
        }
    }

    private fun validateJsonPatchOperations(operations: List<JsonPatchOperation>) {
        val invalidPaths = listOf("/id", "/catalogId", "/conceptId", "/status")
        if (operations.any { it.path in invalidPaths }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patch of paths $invalidPaths is not permitted")
        }
    }

    private fun changeRequestStatusFromString(str: String?): ChangeRequestStatus? =
        when (str?.uppercase()) {
            ChangeRequestStatus.OPEN.name -> ChangeRequestStatus.OPEN
            ChangeRequestStatus.REJECTED.name -> ChangeRequestStatus.REJECTED
            ChangeRequestStatus.ACCEPTED.name -> ChangeRequestStatus.ACCEPTED
            else -> null
        }

}

package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
import java.util.UUID
import no.fdk.concept_catalog.repository.ChangeRequestRepository
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.validation.isOrganizationNumber
import com.fasterxml.jackson.databind.ObjectMapper
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

    fun deleteChangeRequestByConcept(concept: BegrepDBO): Unit =
        changeRequestRepository.getByCatalogIdAndConceptId(concept.ansvarligVirksomhet.id, concept.id)
            .forEach { toDelete -> changeRequestRepository.delete(toDelete) }
            .also { logger.debug("deleted change request with concept id ${concept.id}") }

    fun deleteChangeRequest(id: String, catalogId: String): Unit =
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.let { toDelete -> changeRequestRepository.delete(toDelete) }
            ?.also { logger.debug("deleted change request with id $id") }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

    fun createChangeRequest(catalogId: String, user: User, body: ChangeRequestUpdateBody): String {
        validateNewChangeRequest(body, catalogId, user)

        val newId = UUID.randomUUID().toString()
        ChangeRequest(
            id = newId,
            catalogId = catalogId,
            conceptId = body.conceptId,
            status = ChangeRequestStatus.OPEN,
            operations = body.operations,
            proposedBy = user,
            timeForProposal = Instant.now(),
            title = body.title,
        ).let { changeRequestRepository.save(it) }
            .also { logger.debug("new change request ${it.id} successfully created") }

        return newId
    }

    fun updateChangeRequest(id: String, catalogId: String, user: User, body: ChangeRequestUpdateBody): ChangeRequest? {
        validateJsonPatchOperations(
            getConceptWithFallback(body.conceptId, catalogId, user),
            body.operations
        )

        return changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.copy(
                operations = body.operations,
                title = body.title
            )
            ?.let { changeRequestRepository.save(it) }
            ?.also { logger.debug("updated change request ${it.id}") }
    }

    fun acceptChangeRequest(id: String, catalogId: String, user: User, jwt: Jwt): String {
        val changeRequest = changeRequestRepository.getByIdAndCatalogId(id, catalogId)

        changeRequest?.also { if (it.status != ChangeRequestStatus.OPEN) throw ResponseStatusException(HttpStatus.BAD_REQUEST) }
            ?.copy(status = ChangeRequestStatus.ACCEPTED)
            ?.let { changeRequestRepository.save(it) }
            ?.also { logger.debug("accepted change request ${it.id}") }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)

        val dbConcept = changeRequest.conceptId
            ?.let { conceptRepository.getByOriginaltBegrep(it) }
            ?.maxByOrNull { it.versjonsnr }

        val conceptToUpdate = when {
            dbConcept == null -> createNewConcept(Virksomhet(id=catalogId), user)
                .updateLastChangedAndByWhom(user)
                .let { conceptRepository.save(it) }
            dbConcept.erPublisert -> dbConcept.createNewRevision()
                .updateLastChangedAndByWhom(user)
                .let { conceptRepository.save(it) }
            else -> dbConcept
        }

        try {
            conceptService.updateConcept(conceptToUpdate, changeRequest.operations, user, jwt)
        } catch (ex: Exception) {
            logger.error("update of concept failed when accepting ${changeRequest.id}, reverting acceptation", ex)
            changeRequest.copy(status = ChangeRequestStatus.OPEN).run { changeRequestRepository.save(this) }
            if (conceptToUpdate.id != dbConcept?.id) {
                conceptRepository.delete(conceptToUpdate)
            }
            throw ex
        }

        return conceptToUpdate.id
    }

    fun rejectChangeRequest(id: String, catalogId: String) {
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)
            ?.also { if (it.status != ChangeRequestStatus.OPEN) throw ResponseStatusException(HttpStatus.BAD_REQUEST) }
            ?.copy(status = ChangeRequestStatus.REJECTED)
            ?.let { changeRequestRepository.save(it) }
            ?.also { logger.debug("rejected change request ${it.id}") }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    fun getByIdAndCatalogId(id: String, catalogId: String): ChangeRequest? =
        changeRequestRepository.getByIdAndCatalogId(id, catalogId)

    private fun validateNewChangeRequest(changeRequest: ChangeRequestUpdateBody, catalogId: String, user: User) {
        if (!catalogId.isOrganizationNumber()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided catalogId is not valid organization number")
        }
        if (changeRequest.conceptId != null) {
            val openChangeRequestForConcept = changeRequestRepository.getByConceptIdAndStatus(changeRequest.conceptId, ChangeRequestStatus.OPEN)
            if (openChangeRequestForConcept.isNotEmpty())
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Found unhandled change request for concept")

            val concept = conceptRepository.findByIdOrNull(changeRequest.conceptId)
            if (concept?.ansvarligVirksomhet?.id != catalogId)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "No concept with id ${changeRequest.conceptId} in catalog")
            if (concept.originaltBegrep != changeRequest.conceptId)
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provided conceptId is not the original")
        }

        validateJsonPatchOperations(
            getConceptWithFallback(changeRequest.conceptId, catalogId, user),
            changeRequest.operations
        )
    }

    private fun validateJsonPatchOperationsPaths(operations: List<JsonPatchOperation>) {
        val invalidPaths = listOf("/id", "/catalogId", "/conceptId", "/status")
        if (operations.any { it.path in invalidPaths }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patch of paths $invalidPaths is not permitted")
        }
    }

    private fun validateJsonPatchOperations(concept: BegrepDBO, operations: List<JsonPatchOperation>) {
        try {
            validateJsonPatchOperationsPaths(operations)
            patchOriginal(concept.copy(endringslogelement = null), operations, mapper)
        } catch (ex: Exception) {
            logger.error("failed to validate change request for concept ${concept.id}", ex)
            throw ex
        }
    }

    private fun changeRequestStatusFromString(str: String?): ChangeRequestStatus? =
        when (str?.uppercase()) {
            ChangeRequestStatus.OPEN.name -> ChangeRequestStatus.OPEN
            ChangeRequestStatus.REJECTED.name -> ChangeRequestStatus.REJECTED
            ChangeRequestStatus.ACCEPTED.name -> ChangeRequestStatus.ACCEPTED
            else -> null
        }

    private fun getConceptWithFallback(conceptId: String?, catalogId: String, user: User): BegrepDBO =
        conceptId
            ?.let { conceptService.getLatestVersion(it) }
            ?: createNewConcept(Virksomhet(id=catalogId), user)
}

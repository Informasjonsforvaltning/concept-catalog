package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ChangeRequestService
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException.BadRequest

@RestController
@CrossOrigin
@RequestMapping(value = ["/{catalogId}/endringsforslag"])
class ChangeRequestController(
    private val endpointPermissions: EndpointPermissions,
    private val changeRequestService: ChangeRequestService
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCatalogRequests(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @RequestParam(value = "status") status: String?
    ) : ResponseEntity<List<ChangeRequest>> =
        if (endpointPermissions.hasOrgReadPermission(jwt, catalogId)) {
            ResponseEntity(changeRequestService.getCatalogRequests(catalogId, status), HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }

    @PostMapping
    fun createChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @RequestParam(value = "concept") conceptId: String?
    ) : ResponseEntity<Unit> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null ->  ResponseEntity(HttpStatus.BAD_REQUEST)
            endpointPermissions.hasOrgReadPermission(jwt, catalogId) -> {
                val newId = changeRequestService.createChangeRequest(catalogId, conceptId, user)
                ResponseEntity(locationHeaderForCreated(newId, catalogId), HttpStatus.CREATED)
            }
            else -> ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }

    @PostMapping(value= ["/{changeRequestId}/accept"])
    fun acceptChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable changeRequestId: String
    ) : ResponseEntity<Unit> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgWritePermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)
            else -> {
                val conceptId = changeRequestService.acceptChangeRequest(changeRequestId, catalogId, user, jwt)
                ResponseEntity(locationHeaderForAccepted(conceptId), HttpStatus.OK)
            }
        }
    }

    @PostMapping(value= ["/{changeRequestId}/reject"])
    fun rejectChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable changeRequestId: String
    ) : ResponseEntity<Unit> =
        if (endpointPermissions.hasOrgWritePermission(jwt, catalogId)) {
            changeRequestService.rejectChangeRequest(changeRequestId, catalogId)
            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }

    @GetMapping(value= ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable id: String
    ) : ResponseEntity<ChangeRequest> {
        return if (endpointPermissions.hasOrgReadPermission(jwt, catalogId)) {
            val changeRequest = changeRequestService.getByIdAndCatalogId(id, catalogId)
            if (changeRequest != null) {
                ResponseEntity(changeRequest, HttpStatus.OK)
            } else {
                val conceptChangeRequest = changeRequestService.getByConceptIdAndCatalogId(id, catalogId)
                if (conceptChangeRequest != null) {
                    ResponseEntity(conceptChangeRequest, HttpStatus.OK)
                } else {
                    ResponseEntity(HttpStatus.NOT_FOUND)
                }
            }
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }


    @DeleteMapping(value= ["/{changeRequestId}"])
    fun deleteChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable changeRequestId: String
    ) : ResponseEntity<Unit> =
        if (endpointPermissions.hasOrgWritePermission(jwt, catalogId)) {
            changeRequestService.deleteChangeRequest(changeRequestId, catalogId)
            ResponseEntity(HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }

    @PostMapping(value= ["/{changeRequestId}"])
    fun saveChangeRequestOperations(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable changeRequestId: String,
        @RequestBody patchOperations: List<JsonPatchOperation>
    ) : ResponseEntity<ChangeRequest> =
        if (endpointPermissions.hasOrgReadPermission(jwt, catalogId)) {
            changeRequestService.saveChangeRequestOperations(changeRequestId, catalogId, patchOperations)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }
}

private fun locationHeaderForCreated(newId: String, catalogId: String): HttpHeaders =
    HttpHeaders().apply {
        add(HttpHeaders.LOCATION, "/$catalogId/endringsforslag/$newId")
        add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION)
    }

private fun locationHeaderForAccepted(conceptId: String): HttpHeaders =
    HttpHeaders().apply {
        add(HttpHeaders.LOCATION, "/begreper/$conceptId")
        add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION)
    }

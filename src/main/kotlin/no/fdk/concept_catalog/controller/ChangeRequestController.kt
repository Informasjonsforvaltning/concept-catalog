package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.model.ChangeRequestForCreate
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
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping(value = ["/{catalogId}/endringsforslag"])
class ChangeRequestController(
    private val endpointPermissions: EndpointPermissions,
    private val changeRequestService: ChangeRequestService
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllCatalogRequests(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String
    ) : ResponseEntity<List<ChangeRequest>> =
        if (endpointPermissions.hasOrgReadPermission(jwt, catalogId)) {
            ResponseEntity(changeRequestService.getAllCatalogRequests(catalogId), HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getAllCatalogRequests(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @RequestBody changeRequest: ChangeRequestForCreate
    ) : ResponseEntity<Unit> =
        if (endpointPermissions.hasOrgReadPermission(jwt, catalogId)) {
            val newId = changeRequestService.createChangeRequest(changeRequest, catalogId)
            ResponseEntity(locationHeaderForCreated(newId, catalogId), HttpStatus.CREATED)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
        }

    @GetMapping(value= ["/{changeRequestId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable changeRequestId: String
    ) : ResponseEntity<ChangeRequest> =
        if (endpointPermissions.hasOrgReadPermission(jwt, catalogId)) {
            changeRequestService.getByIdAndCatalogId(changeRequestId, catalogId)
                ?.let { ResponseEntity(it, HttpStatus.OK) }
                ?: ResponseEntity(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity(HttpStatus.FORBIDDEN)
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

    @PatchMapping(value= ["/{changeRequestId}"])
    fun patchChangeRequest(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable changeRequestId: String,
        @RequestBody patchOperations: List<JsonPatchOperation>
    ) : ResponseEntity<ChangeRequest> =
        if (endpointPermissions.hasOrgWritePermission(jwt, catalogId)) {
            changeRequestService.updateChangeRequest(changeRequestId, catalogId, patchOperations)
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
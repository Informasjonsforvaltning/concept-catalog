package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.security.EndpointPermissions
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(value = ["/begreper"])
class ConceptsController(private val endpointPermissions: EndpointPermissions) {

    @PostMapping(
        value = [""],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createBegrep(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody concept: Begrep
    ): ResponseEntity<Unit> =
        if (endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id)) {
            ResponseEntity<Unit>(HttpStatus.CREATED)
        } else ResponseEntity<Unit>(HttpStatus.FORBIDDEN)

    @PostMapping(
        value = ["/import"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createBegreper(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody concepts: List<Begrep>
    ): ResponseEntity<Unit> {
        for (concept in concepts) {
            if (!endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id)) {
                return ResponseEntity<Unit>(HttpStatus.FORBIDDEN)
            }
        }
        return ResponseEntity<Unit>(HttpStatus.CREATED)
    }

    @DeleteMapping(value = ["/{id}"])
    fun deleteBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<Unit> {
        val auth = jwt.claims["authorities"] as? String
        return if (endpointPermissions.hasOrgWritePermission(jwt, auth?.split(":")?.get(1))) {
            ResponseEntity<Unit>(HttpStatus.NO_CONTENT)
        } else ResponseEntity<Unit>(HttpStatus.FORBIDDEN)
    }

    @GetMapping(value = [""], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBegrep(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(
            value = "orgNummer",
            required = true
        ) orgNumber: String,
        @RequestParam(
            value = "status",
            required = false
        ) status: Status?
    ): ResponseEntity<List<Begrep>> =
        if (endpointPermissions.hasOrgReadPermission(jwt, orgNumber)) {
            ResponseEntity<List<Begrep>>(HttpStatus.OK)
        } else ResponseEntity<List<Begrep>>(HttpStatus.FORBIDDEN)

    @GetMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<Begrep> =
        ResponseEntity<Begrep>(HttpStatus.OK)

    @PatchMapping(
        value = ["/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun setBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String,
        @RequestBody patchOperations: List<JsonPatchOperation>,
        @RequestParam(
            value = "validate",
            required = false,
            defaultValue = "true"
        ) validate: Boolean?
    ): ResponseEntity<Begrep> {
        val auth = jwt.claims["authorities"] as? String
        return if (endpointPermissions.hasOrgWritePermission(jwt, auth?.split(":")?.get(1))) {
            ResponseEntity<Begrep>(HttpStatus.OK)
        } else ResponseEntity<Begrep>(HttpStatus.FORBIDDEN)
    }
}

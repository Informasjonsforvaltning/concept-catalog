package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ConceptService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(value = ["/begreper"])
class ConceptsController(
    private val endpointPermissions: EndpointPermissions,
    private val conceptService: ConceptService
) {

    @PostMapping(
        value = [""],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createBegrep(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody concept: Begrep
    ): ResponseEntity<Unit> =
        if (endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id)) {
            conceptService.createConcept(concept)
            ResponseEntity(HttpStatus.CREATED)
        } else ResponseEntity(HttpStatus.FORBIDDEN)

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
        conceptService.createConcepts(concepts)
        return ResponseEntity<Unit>(HttpStatus.CREATED)
    }

    @DeleteMapping(value = ["/{id}"])
    fun deleteBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<Unit> {
        val concept = conceptService.getConceptById(id)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            else -> {
                conceptService.deleteConcept(concept)
                ResponseEntity(HttpStatus.NO_CONTENT)
            }
        }
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
        ) status: String?
    ): ResponseEntity<List<Begrep>> {
        val parsedStatus = conceptService.statusFromString(status)
        return when {
            !endpointPermissions.hasOrgReadPermission(jwt, orgNumber) -> ResponseEntity(HttpStatus.FORBIDDEN)
            status != null && parsedStatus == null -> ResponseEntity(HttpStatus.BAD_REQUEST)
            else -> ResponseEntity(conceptService.getConceptsForOrganization(orgNumber, parsedStatus), HttpStatus.OK)
        }
    }

    @GetMapping(value = ["/{id}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<Begrep> {
        val concept = conceptService.getConceptById(id)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            endpointPermissions.hasOrgReadPermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(concept, HttpStatus.OK)
            else -> ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }

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

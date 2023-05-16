package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.Paginated
import no.fdk.concept_catalog.model.SearchOperation
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ConceptService
import no.fdk.concept_catalog.service.statusFromString
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

private val logger = LoggerFactory.getLogger(ConceptsController::class.java)

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
    ): ResponseEntity<Unit> {
        val userId = endpointPermissions.getUserId(jwt)
        return when {
            userId == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            else -> {
                logger.info("creating concept for ${concept.ansvarligVirksomhet?.id}")
                conceptService.createConcept(concept, userId).id
                    ?.let { ResponseEntity(locationHeaderForCreated(newId = it), HttpStatus.CREATED) }
                    ?: ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @PostMapping(
        value = ["/import"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createBegreper(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody concepts: List<Begrep>
    ): ResponseEntity<Unit> {
        val userId = endpointPermissions.getUserId(jwt)
        return when {
            userId == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            concepts.any { !endpointPermissions.hasOrgWritePermission(jwt, it.ansvarligVirksomhet?.id) } ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            else -> {
                logger.info("creating ${concepts.size} concepts for ${concepts.firstOrNull()?.ansvarligVirksomhet?.id}")
                conceptService.createConcepts(concepts, userId)
                return ResponseEntity<Unit>(HttpStatus.CREATED)
            }
        }
    }

    @PostMapping(value = ["/{id}/revisjon"])
    fun createRevision(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String,
        @RequestBody revision: Begrep
    ): ResponseEntity<Begrep> {
        val concept = conceptService.getConceptDBO(id)
        val userId = endpointPermissions.getUserId(jwt)
        return when {
            userId == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            !concept.erPublisert -> ResponseEntity(HttpStatus.BAD_REQUEST)
            conceptService.findIdOfUnpublishedRevision(concept) != null -> ResponseEntity(HttpStatus.BAD_REQUEST)
            else -> {
                logger.info("creating revision of ${concept.id} for ${concept.ansvarligVirksomhet?.id}")
                conceptService.createRevisionOfConcept(revision, concept, userId).id
                    ?.let { ResponseEntity(locationHeaderForCreated(newId = it), HttpStatus.CREATED) }
                    ?: ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @DeleteMapping(value = ["/{id}"])
    fun deleteBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<Unit> {
        val concept = conceptService.getConceptDBO(id)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            concept.erPublisert -> ResponseEntity(HttpStatus.BAD_REQUEST)
            else -> {
                logger.info("deleting concept $id")
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
        val parsedStatus = statusFromString(status)
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

    @GetMapping(value = ["/{id}/revisions"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getBegrepVersionsById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<List<Begrep>> {
        val concept = conceptService.getConceptDBO(id)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            endpointPermissions.hasOrgReadPermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(conceptService.findRevisions(concept), HttpStatus.OK)
            else -> ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }

    @PatchMapping(
        value = ["/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun patchBegrepById(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String,
        @RequestBody patchOperations: List<JsonPatchOperation>
    ): ResponseEntity<Begrep> {
        val concept = conceptService.getConceptDBO(id)
        val userId = endpointPermissions.getUserId(jwt)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            userId == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet?.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            else -> ResponseEntity(conceptService.updateConcept(concept, patchOperations, userId), HttpStatus.OK)
        }
    }

    @PostMapping(
        value = ["/search"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun searchBegrep(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(
            value = "orgNummer",
            required = true
        ) orgNumber: String,
        @RequestBody searchOperation: SearchOperation
    ): ResponseEntity<Paginated> {
        return when {
            !endpointPermissions.hasOrgReadPermission(jwt, orgNumber) -> ResponseEntity(HttpStatus.FORBIDDEN)
            else -> ResponseEntity(conceptService.searchConcepts(orgNumber, searchOperation), HttpStatus.OK)
        }
    }

}

private fun locationHeaderForCreated(newId: String): HttpHeaders =
    HttpHeaders().apply {
        add(HttpHeaders.LOCATION, "/begreper/$newId")
        add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION)
    }

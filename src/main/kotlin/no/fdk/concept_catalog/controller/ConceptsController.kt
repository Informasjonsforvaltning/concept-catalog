package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.elastic.ElasticUpdater
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.rdf.jenaLangFromHeader
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ChangeRequestService
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
    private val conceptService: ConceptService,
    private val changeRequestService: ChangeRequestService,
    private val elasticUpdater: ElasticUpdater
) {
    @PostMapping(
        value = [""],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun createBegrep(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody concept: Begrep
    ): ResponseEntity<Unit> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                logger.info("creating concept for ${concept.ansvarligVirksomhet.id}")
                conceptService.createConcept(concept, user, jwt).id
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
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            concepts.any { !endpointPermissions.hasOrgAdminPermission(jwt, it.ansvarligVirksomhet.id) } ->
                ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                logger.info("creating ${concepts.size} concepts for ${concepts.firstOrNull()?.ansvarligVirksomhet?.id}")
                conceptService.createConcepts(concepts, user, jwt)
                return ResponseEntity<Unit>(HttpStatus.CREATED)
            }
        }
    }

    @GetMapping(
        value = ["/{catalogId}/count"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun getConceptCount(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String
    ): ResponseEntity<Long> {
        return when {
            !endpointPermissions.hasOrgReadPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)
            else -> ResponseEntity(conceptService.countCurrentConcepts(catalogId), HttpStatus.OK)
        }
    }

    @PostMapping(
        value = ["/{catalogId}/import"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
            "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
    )
    fun createBegreperFromRDF(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @PathVariable catalogId: String,
        @RequestBody concepts: String
    ): ResponseEntity<Void> {
        val user = endpointPermissions.getUser(jwt)

        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                logger.info("Importing RDF concepts for $catalogId")
                conceptService.createConcepts(concepts, jenaLangFromHeader(contentType), user, jwt)

                return ResponseEntity<Void>(HttpStatus.NOT_IMPLEMENTED)
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
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)

            !concept.erPublisert -> ResponseEntity(HttpStatus.BAD_REQUEST)
            conceptService.findIdOfUnpublishedRevision(concept) != null -> ResponseEntity(HttpStatus.BAD_REQUEST)
            else -> {
                logger.info("creating revision of ${concept.id} for ${concept.ansvarligVirksomhet.id}")
                conceptService.createRevisionOfConcept(revision, concept, user, jwt).id
                    ?.let { ResponseEntity(locationHeaderForCreated(newId = it), HttpStatus.CREATED) }
                    ?: ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }

    @PostMapping(value = ["/reindex"])
    fun reindexElastic(@AuthenticationPrincipal jwt: Jwt): ResponseEntity<Unit> {
        return when {
            !endpointPermissions.hasSysAdminPermission(jwt) -> ResponseEntity(HttpStatus.FORBIDDEN)
            else -> {
                logger.info("reindexing elastic")
                elasticUpdater.reindexElastic()
                ResponseEntity(HttpStatus.OK)
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
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)

            concept.erPublisert -> ResponseEntity(HttpStatus.BAD_REQUEST)
            else -> {
                logger.info("deleting concept $id")
                conceptService.deleteConcept(concept)
                changeRequestService.deleteChangeRequestByConcept(concept)
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
            endpointPermissions.hasOrgReadPermission(jwt, concept.ansvarligVirksomhet.id) ->
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
            endpointPermissions.hasOrgReadPermission(jwt, concept.ansvarligVirksomhet.id) ->
                ResponseEntity(conceptService.findRevisions(concept), HttpStatus.OK)

            else -> ResponseEntity(HttpStatus.FORBIDDEN)
        }
    }

    @PostMapping(value = ["/{id}/publish"])
    fun publish(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: String
    ): ResponseEntity<Begrep> {
        val concept = conceptService.getConceptDBO(id)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet.id) -> {
                ResponseEntity(conceptService.publish(concept), HttpStatus.OK)
            }

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
        val user = endpointPermissions.getUser(jwt)
        return when {
            concept == null -> ResponseEntity(HttpStatus.NOT_FOUND)
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgWritePermission(jwt, concept.ansvarligVirksomhet.id) ->
                ResponseEntity(HttpStatus.FORBIDDEN)

            concept.erPublisert -> {
                logger.info("creating revision of ${concept.id} for ${concept.ansvarligVirksomhet.id}")
                conceptService.createRevisionOfConcept(patchOperations, concept, user, jwt).id
                    ?.let { ResponseEntity(locationHeaderForCreated(newId = it), HttpStatus.CREATED) }
                    ?: ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
            }

            else -> ResponseEntity(conceptService.updateConcept(concept, patchOperations, user, jwt), HttpStatus.OK)
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

    @GetMapping(
        value = ["/suggestions"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun suggestBegrep(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(
            value = "org",
            required = true
        ) orgNumber: String,
        @RequestParam(
            value = "published",
            required = false
        ) published: Boolean?,
        @RequestParam(
            value = "q"
        ) query: String
    ): ResponseEntity<List<Suggestion>> {
        return when {
            !endpointPermissions.hasOrgReadPermission(jwt, orgNumber) -> ResponseEntity(HttpStatus.FORBIDDEN)
            else -> ResponseEntity(conceptService.suggestConcepts(orgNumber, published, query), HttpStatus.OK)
        }
    }

}

private fun locationHeaderForCreated(newId: String): HttpHeaders =
    HttpHeaders().apply {
        add(HttpHeaders.LOCATION, "/begreper/$newId")
        add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION)
    }

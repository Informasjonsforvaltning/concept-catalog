package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.rdf.jenaLangFromHeader
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ImportService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.net.URI

@CrossOrigin
@RestController
@RequestMapping(value = ["/import/{catalogId}"])
class ImportController(private val endpointPermissions: EndpointPermissions, private val importService: ImportService) {

    @PostMapping(
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
            "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
    )
    fun import(
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
                logger.info("RDF import for catalog {}", catalogId)

                val importStatus = importService.importRdf(
                    catalogId = catalogId,
                    concepts = concepts,
                    lang = jenaLangFromHeader(contentType),
                    user = user,
                    jwt = jwt
                )

                return ResponseEntity
                    .created(URI("/import/$catalogId/${importStatus.id}"))
                    .build()
            }
        }
    }

    @GetMapping(
        value = ["/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun status(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable id: String,
    ): ResponseEntity<ImportResult> {
        val user = endpointPermissions.getUser(jwt)

        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                val importStatus = importService.getStatus(id)

                return ResponseEntity.ok(importStatus)
            }
        }
    }
}

private val logger: Logger = LoggerFactory.getLogger(ImportController::class.java)

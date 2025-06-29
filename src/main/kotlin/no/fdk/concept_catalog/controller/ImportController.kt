package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.rdf.jenaLangFromHeader
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ImportService
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
                val importStatus = importService.importRdf(
                    catalogId = catalogId,
                    concepts = concepts,
                    lang = jenaLangFromHeader(contentType),
                    user = user,
                    jwt = jwt
                )

                return ResponseEntity
                    .created(URI("/import/$catalogId/results/${importStatus.id}"))
                    .build()
            }
        }
    }

    @GetMapping(
        value = ["/results"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun result(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
    ): ResponseEntity<List<ImportResult>> {
        val user = endpointPermissions.getUser(jwt)

        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                return ResponseEntity.ok(importService.getResults(catalogId))
            }
        }
    }

    @GetMapping(
        value = ["/results/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun result(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable id: String,
    ): ResponseEntity<ImportResult> {
        val user = endpointPermissions.getUser(jwt)

        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                importService.getResult(id)
                    ?.let { ResponseEntity.ok(it) }
                    ?: ResponseEntity(HttpStatus.NOT_FOUND)
            }
        }
    }

    @DeleteMapping(
        value = ["/results/{id}"]
    ) fun deleteResult(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable id: String): ResponseEntity<Void> {

        val user = endpointPermissions.getUser(jwt)

        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> importService.deleteImportResult(catalogId, id).let {
                ResponseEntity
                    .noContent()
                    .build()
            }
        }
    }
}

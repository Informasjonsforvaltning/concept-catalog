package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.rdf.jenaLangFromHeader
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ImportService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.concurrent.Executor

@CrossOrigin
@RestController
@RequestMapping(value = ["/import/{catalogId}"])
class ImportController(@Qualifier("import-executor") private val importExecutor: Executor,
                       private val endpointPermissions: EndpointPermissions, private val importService: ImportService) {
    private val logger: Logger = LoggerFactory.getLogger(ImportService::class.java)

    @PutMapping(value = ["/{importId}/cancel"])
    fun cancelImport(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable importId: String
    ): ResponseEntity<String> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) ->
                ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                importService.cancelImport(importId)
                ResponseEntity
                    .created(URI("/import/$catalogId/results/${importId}"))
                    .build()
            }
        }
    }

    @PutMapping(value = ["/{importId}/confirm"])
    fun confirmImport(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable importId: String
    ): ResponseEntity<String> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                importService.confirmImportAndSave(catalogId, importId, user, jwt)
                return ResponseEntity
                    .created(URI("/import/$catalogId/results/${importId}"))
                    .build()
            }
        }
    }

    @GetMapping(
        value = ["/createImportId"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    fun createImportId(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String
    ): ResponseEntity<String> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> {
                logger.info("User for creating import is null (Unauthorized)")
                ResponseEntity(HttpStatus.UNAUTHORIZED)
            }
            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> {
                logger.info("User for creating import is not allowed (forbidden)")
                ResponseEntity(HttpStatus.FORBIDDEN)
            }


            else -> {
                logger.info("Creating import is not allowed (Unauthorized)")
                val importResult = importService.createImportResult(catalogId)
                return ResponseEntity
                    .created(URI("/import/$catalogId/results/${importResult.id}"))
                    .build()
            }
        }
    }

    @PostMapping(
        value = ["/{importId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
            "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
    )
    fun import(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestHeader(HttpHeaders.CONTENT_TYPE) contentType: String,
        @PathVariable catalogId: String,
        @PathVariable importId: String,
        @RequestBody concepts: String
    ): ResponseEntity<Void> {
        val user = endpointPermissions.getUser(jwt)

        return when {
            user == null -> {
                logger.info("User for importing is null (Unauthorized)")
                ResponseEntity(HttpStatus.UNAUTHORIZED)
            }

            !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) -> {
                logger.info("User for importing is not allowed (forbidden)")
                ResponseEntity(HttpStatus.FORBIDDEN)
            }

            else -> {

                logger.info("Importing RDF data now")
                importExecutor.execute {
                    importService.importRdf(
                        catalogId = catalogId,
                        importId = importId,
                        concepts = concepts,
                        lang = jenaLangFromHeader(contentType),
                        user = user,
                        jwt = jwt
                    )
                }

                ResponseEntity
                    .created(URI("/import/$catalogId/results/${importId}"))
                    .build()
            }
        }
    }

    @PostMapping(
        value = ["/{importId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun importBegreper(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable catalogId: String,
        @PathVariable importId: String,
        @RequestBody concepts: List<Begrep>
    ): ResponseEntity<Unit> {
        val user = endpointPermissions.getUser(jwt)
        return when {
            user == null -> ResponseEntity(HttpStatus.UNAUTHORIZED)
            concepts.any { !endpointPermissions.hasOrgAdminPermission(jwt, catalogId) } ->
                ResponseEntity(HttpStatus.FORBIDDEN)
            concepts.any { it?.ansvarligVirksomhet?.id != catalogId } -> ResponseEntity(HttpStatus.FORBIDDEN)

            else -> {
                val importResult = importService.importConcepts(concepts, catalogId, user, jwt, importId)
                return ResponseEntity
                    .created(URI("/import/$catalogId/results/${importResult.id}"))
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

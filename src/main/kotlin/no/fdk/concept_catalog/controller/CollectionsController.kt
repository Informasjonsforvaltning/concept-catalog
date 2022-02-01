package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.model.Begrepssamling
import no.fdk.concept_catalog.security.EndpointPermissions
import no.fdk.concept_catalog.service.ConceptService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
@RequestMapping(value = ["/begrepssamlinger"])
class CollectionsController(
    private val endpointPermissions: EndpointPermissions,
    private val conceptService: ConceptService
) {

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPermittedCollections(
        @AuthenticationPrincipal jwt: Jwt
    ): ResponseEntity<List<Begrepssamling>> {
        return when {
            endpointPermissions.hasSysAdminPermission(jwt) ->
                ResponseEntity(conceptService.getAllCollections(), HttpStatus.OK)
            else -> ResponseEntity(
                conceptService.getCollectionsForOrganizations(
                    endpointPermissions.getOrgsByPermission(jwt, "read")
                ), HttpStatus.OK
            )
        }
    }

}

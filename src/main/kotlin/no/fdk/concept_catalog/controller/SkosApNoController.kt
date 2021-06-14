package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.rdf.jenaLangFromAcceptHeader
import no.fdk.concept_catalog.rdf.rdfResponse
import no.fdk.concept_catalog.service.SkosApNoModelService
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(
    value = ["/collections"],
    produces = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml", "application/n-triples"]
)
class SkosApNoController(private val skosApNoModelService: SkosApNoModelService) {

    @GetMapping("/{id}")
    fun getCollectionById(@RequestHeader(HttpHeaders.ACCEPT) accept: String?, @PathVariable("id") id: String): String =
        skosApNoModelService.buildModelForPublishersCollection(id)
            .rdfResponse(jenaLangFromAcceptHeader(accept))

    @GetMapping
    fun getAllCollections(@RequestHeader(HttpHeaders.ACCEPT) accept: String?): String =
        skosApNoModelService.buildModelForAllCollections()
            .rdfResponse(jenaLangFromAcceptHeader(accept))

}

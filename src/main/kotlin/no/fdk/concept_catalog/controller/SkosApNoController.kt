package no.fdk.concept_catalog.controller

import no.fdk.concept_catalog.rdf.turtleResponse
import no.fdk.concept_catalog.service.SkosApNoModelService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping(value = ["/collections"], produces = ["text/turtle"])
class SkosApNoController(private val skosApNoModelService: SkosApNoModelService) {

    @GetMapping("/{id}")
    fun getCollectionById(@PathVariable("id") id: String): String =
        skosApNoModelService.buildModelForPublishersCollection(id)
            .turtleResponse()

    @GetMapping
    fun getAllCollections() : String =
        skosApNoModelService.buildModelForAllCollections()
            .turtleResponse()

}

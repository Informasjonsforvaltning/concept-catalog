package no.fdk.concept_catalog.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class ApplicationStatusController() {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Unit> =
        ResponseEntity.ok().build()

    @GetMapping("/ready")
    fun ready(): ResponseEntity<Unit> =
        ResponseEntity(HttpStatus.OK)

}

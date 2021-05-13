package no.fdk.concept_catalog

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.SpringApplication

@SpringBootApplication
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

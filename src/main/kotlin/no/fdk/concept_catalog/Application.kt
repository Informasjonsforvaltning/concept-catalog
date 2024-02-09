package no.fdk.concept_catalog

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableElasticsearchRepositories(basePackages = ["no.fdk.concept_catalog.elastic"])
@EnableMongoRepositories(basePackages = ["no.fdk.concept_catalog.repository"])
@EnableScheduling
@EnableWebSecurity
open class Application

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java, *args)
}

package no.fdk.concept_catalog.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application")
data class ApplicationProperties(
    val collectionBaseUri: String,
    val historyServiceUri: String,
    val adminServiceUri: String
)

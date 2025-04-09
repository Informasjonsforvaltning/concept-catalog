package no.fdk.concept_catalog.elastic

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("application.elasticsearch")
data class ElasticProperties (
    val username: String,
    val password: String,
    val host: String,
    val ssl: Boolean,
    val storePath: String,
    val storePass: String
)

package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bruker (
    val id: String,
    val navn: String? = null,
    val email: String? = null
)

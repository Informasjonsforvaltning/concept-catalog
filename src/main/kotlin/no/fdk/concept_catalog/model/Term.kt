package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Term (
    val navn: Map<String, Any>? = HashMap()
)

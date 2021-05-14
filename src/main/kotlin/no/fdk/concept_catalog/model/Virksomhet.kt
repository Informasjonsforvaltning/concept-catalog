package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Virksomhet (
    val uri: String? = null,
    val id: String? = null,
    val navn: String? = null,
    val orgPath: String? = null,
    val prefLabel: String? = null
)

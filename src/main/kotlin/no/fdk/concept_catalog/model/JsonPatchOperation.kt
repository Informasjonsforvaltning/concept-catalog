package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonPatchOperation (
    val op: OpEnum,
    val path: String,
    val value: Any? = null,
    val from: String? = null
)

package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Definisjon (
    val tekst: Map<String, String>? = HashMap(),
    val kildebeskrivelse: Kildebeskrivelse?
)

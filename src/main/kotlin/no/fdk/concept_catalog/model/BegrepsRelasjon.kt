package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class BegrepsRelasjon (
    val relasjon: String? = null,
    val relasjonsType: String? = null,
    val beskrivelse: Map<String, String>? = null,
    val inndelingskriterium: Map<String, String>? = null,
    val relatertBegrep: String? = null
)

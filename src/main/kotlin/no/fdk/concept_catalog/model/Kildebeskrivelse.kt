package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kildebeskrivelse (
    val forholdTilKilde: ForholdTilKildeEnum,
    val kilde: List<URITekst> = ArrayList()
)

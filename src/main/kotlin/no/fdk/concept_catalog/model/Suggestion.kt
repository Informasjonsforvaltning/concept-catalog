package no.fdk.concept_catalog.model

data class Suggestion(
    val id: String,
    val originaltBegrep: String,
    val erPublisert: Boolean,
    val anbefaltTerm: Term?,
    val definisjon: Definisjon?
)

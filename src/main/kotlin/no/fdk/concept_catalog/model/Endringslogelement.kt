package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class Endringslogelement(
    val brukerId: String,
    val endringstidspunkt: LocalDateTime
)

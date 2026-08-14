package no.fdk.conceptcatalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kontaktpunkt(val harEpost: String? = null, val harTelefon: String? = null)

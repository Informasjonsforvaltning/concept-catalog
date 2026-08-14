package no.fdk.conceptcatalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class URITekst(val uri: String? = null, val tekst: String? = null)

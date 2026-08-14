package no.fdk.conceptcatalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InterntFelt(val value: String?)

package no.fdk.conceptcatalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Term(val navn: Map<String, String> = HashMap())

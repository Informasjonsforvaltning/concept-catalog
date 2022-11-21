package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
class SearchOperation(
    val query: String?,
    val fields: QueryFields = QueryFields(),
    val filters: SearchFilters? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
class QueryFields(
    val anbefaltTerm: Boolean = true,
    val definisjon: Boolean = true
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchFilters(
    val status: SearchFilter? = null,
    val onlyCurrentVersions: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchFilter(
    val value: String
)

package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
class SearchOperation(
    val query: String?,
    val fields: QueryFields = QueryFields(),
    val filters: SearchFilters? = null,
    val sort: SortField = SortField(),
    val pagination: Pagination = Pagination()
)

@JsonIgnoreProperties(ignoreUnknown = true)
class QueryFields(
    val anbefaltTerm: Boolean = true,
    val frarådetTerm: Boolean = true,
    val tillattTerm: Boolean = true,
    val definisjon: Boolean = true,
    val merknad: Boolean = true
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchFilters(
    val status: SearchFilter? = null,
    val onlyCurrentVersions: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchFilter(
    val value: List<String>
)

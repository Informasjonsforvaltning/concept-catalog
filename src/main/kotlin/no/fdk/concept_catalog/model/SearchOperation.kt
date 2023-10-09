package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


@JsonIgnoreProperties(ignoreUnknown = true)
class SearchOperation(
    val query: String?,
    val fields: QueryFields = QueryFields(),
    val filters: SearchFilters = SearchFilters(),
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
    val assignedUser: SearchFilter<List<String>>? = null,
    val status: SearchFilter<List<String>>? = null,
    val published: BooleanFilter? = null,
    val subject: SearchFilter<List<String>>? = null,
    val originalId: SearchFilter<List<String>>? = null,
    val internalFields: SearchFilter<Map<String, List<String>>>? = null,
    val label: SearchFilter<List<String>>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class SearchFilter<T>(
    val value: T
)

@JsonIgnoreProperties(ignoreUnknown = true)
class BooleanFilter(
    val value: Boolean
)

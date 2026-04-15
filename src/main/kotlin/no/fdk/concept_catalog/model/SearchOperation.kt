package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty


@JsonIgnoreProperties(ignoreUnknown = true)
class SearchOperation(
    val query: String?,
    @JsonProperty("fields", required = false)
    fields: QueryFields? = null,
    @JsonProperty("filters", required = false)
    filters: SearchFilters? = null,
    val sort: SortField? = null,
    @JsonProperty("pagination", required = false)
    pagination: Pagination? = null,
) {
    val fields: QueryFields = fields ?: QueryFields()
    val filters: SearchFilters = filters ?: SearchFilters()
    val pagination: Pagination = pagination ?: Pagination()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class QueryFields(
    anbefaltTerm: Boolean? = null,
    frarådetTerm: Boolean? = null,
    tillattTerm: Boolean? = null,
    definisjon: Boolean? = null,
    merknad: Boolean? = null,
) {
    val anbefaltTerm: Boolean = anbefaltTerm ?: true
    val frarådetTerm: Boolean = frarådetTerm ?: true
    val tillattTerm: Boolean = tillattTerm ?: true
    val definisjon: Boolean = definisjon ?: true
    val merknad: Boolean = merknad ?: true
}

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

package no.fdk.concept_catalog.service

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import no.fdk.concept_catalog.model.SearchFilters

fun SearchFilters.asQueryFilters(orgNumber: String): List<Query> {
    val queryFilters = mutableListOf(Query.of{
            x -> x.match{m -> m.field("ansvarligVirksomhet.id").query(orgNumber) }
    })
    if (published != null) {
        queryFilters.add(Query.of{x -> x.term{ t -> t.field ("erPublisert").value(FieldValue.of(published.value)) }})
    }
    return queryFilters
}
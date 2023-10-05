package no.fdk.concept_catalog.service

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import no.fdk.concept_catalog.model.SearchFilters

fun SearchFilters.asQueryFilters(orgNumber: String): List<Query> {
    val queryFilters = mutableListOf(Query.of { queryBuilder ->
        queryBuilder.term { termBuilder -> termBuilder.field("ansvarligVirksomhet.id.keyword").value(orgNumber) }
    })
    if (published != null) {
        queryFilters.add(Query.of { queryBuilder ->
            queryBuilder.term { termBuilder ->
                termBuilder.field("erPublisert").value(FieldValue.of(published.value))
            }
        })
    }
    if (assignedUser != null) {
        queryFilters
            .add(Query.of { queryBuilder ->
                queryBuilder.terms { termsBuilder ->
                    termsBuilder.field("assignedUser.keyword")
                        .terms { fieldBuilder -> fieldBuilder.value(assignedUser.value.map { FieldValue.of(it) }) }
                }
            })
    }
    return queryFilters
}

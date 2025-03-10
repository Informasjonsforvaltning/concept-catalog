package no.fdk.concept_catalog.service

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import no.fdk.concept_catalog.model.SearchFilters

fun orgFilter(orgNumber: String): Query =
    Query.of { queryBuilder ->
        queryBuilder.term { termBuilder -> termBuilder.field("ansvarligVirksomhet.id.keyword").value(orgNumber) }
    }

fun SearchFilters.asQueryFilters(orgNumber: String): List<Query> {
    val queryFilters = mutableListOf(orgFilter(orgNumber))

    if (status != null) {
        queryFilters
            .add(Query.of { queryBuilder ->
                queryBuilder.terms { termsBuilder ->
                    termsBuilder.field("statusURI.keyword")
                        .terms { fieldBuilder -> fieldBuilder.value(status.value.map { FieldValue.of(it) }) }
                }
            })
    }

    if (published != null) {
        queryFilters.add(Query.of { queryBuilder ->
            queryBuilder.bool { boolBuilder ->
                if (published.value) {
                    boolBuilder.must { mustBuilder ->
                        mustBuilder.exists { existsBuilder -> existsBuilder.field("sistPublisertId") }
                    }
                } else {
                    boolBuilder.mustNot { mustNotBuilder ->
                        mustNotBuilder.exists { existsBuilder -> existsBuilder.field("sistPublisertId") }
                    }
                }
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

    if (originalId != null) {
        queryFilters
            .add(Query.of { queryBuilder ->
                queryBuilder.terms { termsBuilder ->
                    termsBuilder.field("originaltBegrep.keyword")
                        .terms { fieldBuilder -> fieldBuilder.value(originalId.value.map { FieldValue.of(it) }) }
                }
            })
    }

    if (label != null) {
        queryFilters
            .add(Query.of { queryBuilder ->
                queryBuilder.terms { termsBuilder ->
                    termsBuilder.field("merkelapp.keyword")
                        .terms { fieldBuilder -> fieldBuilder.value(label.value.map { FieldValue.of(it) }) }
                }
            })
    }

    if (subject != null) {
        queryFilters
            .add(Query.of { queryBuilder ->
                queryBuilder.terms { termsBuilder ->
                    termsBuilder.field("fagområdeKoder.keyword")
                        .terms { fieldBuilder -> fieldBuilder.value(subject.value.map { FieldValue.of(it) }) }
                }
            })
    }

    internalFields?.value?.forEach { (key, value) ->
        queryFilters
            .add(Query.of { queryBuilder ->
                queryBuilder.terms { termsBuilder ->
                    termsBuilder.field("interneFelt.$key.value.keyword")
                        .terms { fieldBuilder -> fieldBuilder.value(value.map { FieldValue.of(it) }) }
                }
            })
    }

    return queryFilters
}

fun suggestionFilters(orgNumber: String, published: Boolean?): List<Query> {
    val filters = mutableListOf(orgFilter(orgNumber))

    if (published != null) {
        filters.add(Query.of { queryBuilder ->
            queryBuilder.term { termBuilder ->
                termBuilder.field("erPublisert").value(FieldValue.of(published))
            }
        })
    }

    return filters
}

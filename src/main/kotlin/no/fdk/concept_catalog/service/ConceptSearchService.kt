package no.fdk.concept_catalog.service

import co.elastic.clients.elasticsearch._types.ScriptSortType
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import no.fdk.concept_catalog.model.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Service

@Service
class ConceptSearchService(
    private val elasticsearchOperations: ElasticsearchOperations
) {

    private val logger = LoggerFactory.getLogger(ConceptSearchService::class.java)

    fun suggestConcepts(orgNumber: String, published: Boolean?, query: String): SearchHits<CurrentConcept> =
        elasticsearchOperations.search(
            suggestionQuery(orgNumber, published, query),
            CurrentConcept::class.java,
            IndexCoordinates.of("concepts-current")
        )

    fun searchCurrentConcepts(orgNumber: String, search: SearchOperation): SearchHits<CurrentConcept> {
        val query = search.toElasticQuery(orgNumber)
        return elasticsearchOperations.search(
            query,
            CurrentConcept::class.java,
            IndexCoordinates.of("concepts-current")
        )
    }

    private fun suggestionQuery(orgNumber: String, published: Boolean?, query: String): Query {
        val builder = NativeQuery.builder()
        builder.withFilter { queryBuilder ->
            queryBuilder.bool { boolBuilder ->
                boolBuilder.must(suggestionFilters(orgNumber, published))
            }
        }
        builder.withQuery { queryBuilder ->
            queryBuilder.matchPhrasePrefix { matchBuilder ->
                matchBuilder.query(query)
                    .field("anbefaltTerm.navn.nb")
            }
        }
        return builder.build()
    }

    private fun SortField.buildSort(builder: NativeQueryBuilder) {
        if (field == SortFieldEnum.ANBEFALT_TERM) {
            builder.withSort { sortBuilder ->
                sortBuilder.field { fieldBuilder ->
                    fieldBuilder.field("anbefaltTerm_sort").order(sortDirection())
                }
            }
        } else {
            builder.withSort { sortBuilder ->
                sortBuilder.field { fieldBuilder ->
                    fieldBuilder.field("endringslogelement.endringstidspunkt").order(sortDirection())
                }
            }
        }
    }

    private fun SearchOperation.toElasticQuery(orgNumber: String): Query {
        val builder = NativeQuery.builder()
        builder.withFilter { queryBuilder ->
            queryBuilder.bool { boolBuilder ->
                boolBuilder.must(
                    filters.asQueryFilters(
                        orgNumber
                    )
                )
            }
        }
        sort?.buildSort(builder)
        if (!query.isNullOrBlank()) builder.addFieldsQuery(fields, query)
        builder.withPageable(Pageable.ofSize(pagination.getSize()).withPage(pagination.getPage()))

        return builder.build()
    }


    private fun NativeQueryBuilder.addFieldsQuery(queryFields: QueryFields, queryValue: String) {
        withQuery { queryBuilder ->
            queryBuilder.bool { boolBuilder ->
                boolBuilder.should {
                    it.multiMatch { matchBuilder ->
                        matchBuilder.fields(queryFields.exactPaths())
                            .query(queryValue)
                            .operator(Operator.And)
                            .type(TextQueryType.Phrase)
                    }
                }

                boolBuilder.should {
                    it.multiMatch { matchBuilder ->
                        matchBuilder.fields(queryFields.prefixPaths())
                            .query(queryValue)
                            .operator(Operator.And)
                            .type(TextQueryType.BoolPrefix)
                    }
                }

                boolBuilder.minimumShouldMatch("1")
            }
        }
    }

    private fun SortField.sortDirection(): SortOrder =
        when (direction) {
            SortDirection.ASC -> SortOrder.Asc
            else -> SortOrder.Desc
        }

    private fun QueryFields.exactPaths(): List<String> =
        listOf(
            if (anbefaltTerm) languagePaths("anbefaltTerm.navn", 30)
            else emptyList(),

            if (frarådetTerm) languagePaths("frarådetTerm", 10)
            else emptyList(),

            if (tillattTerm) languagePaths("tillattTerm", 10)
            else emptyList(),

            if (definisjon) languagePaths("definisjon.tekst")
            else emptyList(),

            if (merknad) languagePaths("merknad")
            else emptyList()
        ).flatten()

    private fun QueryFields.prefixPaths(): List<String> =
        listOf(
            if (anbefaltTerm) languagePaths("anbefaltTerm.navn", 15)
            else emptyList(),

            if (frarådetTerm) languagePaths("frarådetTerm", 5)
            else emptyList(),

            if (tillattTerm) languagePaths("tillattTerm", 5)
            else emptyList(),

            if (definisjon) languagePaths("definisjon.tekst")
            else emptyList(),

            if (merknad) languagePaths("merknad")
            else emptyList()
        ).flatten()

    private fun languagePaths(basePath: String, boost: Int? = null): List<String> =
        listOf("$basePath.nb${if (boost != null) "^$boost" else ""}",
            "$basePath.nn${if (boost != null) "^$boost" else ""}",
            "$basePath.en${if (boost != null) "^$boost" else ""}")

}



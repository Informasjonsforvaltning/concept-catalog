package no.fdk.concept_catalog.service

import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import no.fdk.concept_catalog.model.*
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

    fun searchCurrentConcepts(orgNumber: String, search: SearchOperation): SearchHits<CurrentConcept> =
        elasticsearchOperations.search(
            search.toElasticQuery(orgNumber),
            CurrentConcept::class.java,
            IndexCoordinates.of("concepts-current")
        )

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
        builder.withSort { sortBuilder ->
            sortBuilder.field { fieldBuilder ->
                fieldBuilder.field(sort.sortField()).order(sort.sortDirection())
            }
        }
        if (!query.isNullOrBlank()) builder.addFieldsQuery(fields, query)
        builder.withPageable(Pageable.ofSize(pagination.getSize()).withPage(pagination.getPage()))
        return builder.build()
    }

    private fun NativeQueryBuilder.addFieldsQuery(queryFields: QueryFields, queryValue: String) {
        withQuery { queryBuilder ->
            queryBuilder.multiMatch { matchBuilder ->
                matchBuilder.fields(queryFields.paths())
                    .query(queryValue)
                    .type(TextQueryType.BoolPrefix)
            }
        }
    }

    private fun SortField.sortDirection(): SortOrder =
        when (direction) {
            SortDirection.ASC -> SortOrder.Asc
            else -> SortOrder.Desc
        }

    private fun SortField.sortField(): String =
        when (field) {
            SortFieldEnum.ANBEFALT_TERM_NB -> "anbefaltTerm.navn.nb.keyword"
            else -> "endringslogelement.endringstidspunkt"
        }

    private fun QueryFields.paths(): List<String> =
        listOf(
            // Boosting hits in anbefaltTerm
            if (anbefaltTerm) listOf(
                "anbefaltTerm.navn.nb^10",
                "anbefaltTerm.navn.en^5",
                "anbefaltTerm.navn.nn^5"
            )
            else emptyList(),

            if (frarådetTerm) languagePaths("frarådetTerm")
            else emptyList(),

            if (tillattTerm) languagePaths("tillattTerm")
            else emptyList(),

            if (definisjon) languagePaths("definisjon.tekst")
            else emptyList(),

            if (merknad) languagePaths("merknad")
            else emptyList()
        ).flatten()

    private fun languagePaths(basePath: String): List<String> =
        listOf("$basePath.nb", "$basePath.en", "$basePath.nn")

}

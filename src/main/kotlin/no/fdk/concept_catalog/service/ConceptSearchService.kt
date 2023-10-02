package no.fdk.concept_catalog.service

import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import no.fdk.concept_catalog.elastic.ConceptSearchRepository
import no.fdk.concept_catalog.model.*
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.stereotype.Service

@Service
class ConceptSearchService(
    private val conceptRepository: MongoTemplate,
    private val conceptSearchRepository: ConceptSearchRepository,
    private val elasticsearchOperations: ElasticsearchOperations
) {

    fun reindexElastic() {
        conceptSearchRepository.deleteAll()
        conceptSearchRepository.saveAll(conceptRepository.findAll<BegrepDBO>())
    }

    fun searchConcepts(orgNumber: String, search: SearchOperation): List<BegrepDBO> =
        elasticsearchOperations.search(
            search.toElasticQuery(orgNumber),
            BegrepDBO::class.java,
            IndexCoordinates.of("concepts")
        ).map { it.content }.toList()

    private fun SearchOperation.toElasticQuery(orgNumber: String): Query {
        val qb = NativeQuery.builder()
        qb.withFilter { q -> q.match { m -> m.field("ansvarligVirksomhet.id").query(orgNumber) } }
        if (!query.isNullOrBlank()) qb.addFieldsQuery(fields, query)
        return qb.build()
    }

    private fun NativeQueryBuilder.addFieldsQuery(queryFields: QueryFields, queryValue: String) {
        withQuery { q ->
            q.multiMatch { mm ->
                mm.fields(queryFields.paths())
                    .query(queryValue)
                    .type(TextQueryType.BoolPrefix)
            }
        }
    }

    private fun QueryFields.paths(): List<String> =
        listOf(
            if (anbefaltTerm) boostedLanguagePaths("anbefaltTerm.navn")
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

    private fun boostedLanguagePaths(basePath: String): List<String> =
        listOf("$basePath.nb^5", "$basePath.en^2", "$basePath.nn^2")

    private fun languagePaths(basePath: String): List<String> =
        listOf("$basePath.nb", "$basePath.en", "$basePath.nn")

}

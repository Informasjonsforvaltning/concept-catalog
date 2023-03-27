package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.QueryFields
import no.fdk.concept_catalog.model.SearchOperation
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class ConceptSearchService(
    private val conceptRepository: MongoTemplate
) {

    fun searchConcepts(orgNumber: String, search: SearchOperation): List<BegrepDBO> =
        conceptRepository.find(search.toMongoQuery(orgNumber))

    private fun SearchOperation.toMongoQuery(orgNumber: String): Query {
        val searchCriteria = Criteria.where("ansvarligVirksomhet.id").`is`(orgNumber)

        if (query != null) searchCriteria.orOperator(fields.queryCriteria(query))

        if (filters?.status != null) {
            val status = statusFromString(filters.status.value)
            searchCriteria.andOperator(
                Criteria.where("status").`is`(status)
            )
        }

        return Query(searchCriteria)
    }

    private fun QueryFields.queryCriteria(query: String): List<Criteria> =
        listOf(
            if (anbefaltTerm) {
                languageCriteria(langPath = "anbefaltTerm.navn", query = query)
            } else emptyList(),

            if (frarådetTerm) {
                languageCriteria(langPath = "frarådetTerm", query = query)
            } else emptyList(),

            if (tillattTerm) {
                languageCriteria(langPath = "tillattTerm", query = query)
            } else emptyList(),

            if (definisjon) {
                languageCriteria(langPath = "definisjon.tekst", query = query)
            } else emptyList(),

            if (merknad) {
                languageCriteria(langPath = "merknad", query = query)
            } else emptyList()
        ).flatten()

    private fun languageCriteria(langPath: String, query: String): List<Criteria> =
        listOf(
            regexCriteria(field = "$langPath.nb", query = query),
            regexCriteria(field = "$langPath.en", query = query),
            regexCriteria(field = "$langPath.nn", query = query)
        )

    private fun regexCriteria(field: String, query: String) =
        // options: "i" for case-insensitive match
        Criteria.where(field).regex(query, "i")
}

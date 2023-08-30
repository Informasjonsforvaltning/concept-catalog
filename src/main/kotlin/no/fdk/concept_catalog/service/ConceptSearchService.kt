package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
import org.springframework.data.domain.Sort
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
        conceptRepository.find(search.toMongoQuery(orgNumber)
            .with(search.sort.toSort()))

    private fun SearchOperation.toMongoQuery(orgNumber: String): Query {
        val searchCriteria = Criteria.where("ansvarligVirksomhet.id").`is`(orgNumber)

        if (!query.isNullOrBlank()) searchCriteria.orOperator(fields.queryCriteria(query))
        val mongoQuery = Query(searchCriteria)

        if (filters.status != null) {
            mongoQuery.addCriteria(Criteria.where("statusURI").`in`(filters.status.value))
        }

        if (filters.published != null) {
            // Use not equal to 'true' instead of is 'false', to also get hits on erPublisert with null value
            if (filters.published.value) mongoQuery.addCriteria(Criteria.where("erPublisert").`is`(true))
            else mongoQuery.addCriteria(Criteria.where("erPublisert").ne(true))
        }

        if (filters.assignedUser != null) {
            mongoQuery.addCriteria(Criteria.where("assignedUser").`in`(filters.assignedUser.value))
        }

        if (filters.subject != null) {
            mongoQuery.addCriteria(Criteria.where("fagområdeKoder").`in`(filters.subject.value))
        }

        if (filters.originalId != null) {
            mongoQuery.addCriteria(Criteria.where("originaltBegrep").`in`(filters.originalId.value))
        }

        if(filters.internalFields != null) {
            filters.internalFields.value.forEach { (key, value) ->
                mongoQuery.addCriteria(Criteria.where("interneFelt.$key.value").`in`(value))
            }
        }

        return mongoQuery
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

    private fun SortField.toSort(): Sort =
        when (field) {
            SortFieldEnum.ANBEFALT_TERM_NB ->
                Sort.by(sortDirection(), "anbefaltTerm.navn.nb")
            else -> Sort.by(sortDirection(), "endringslogelement.endringstidspunkt")
        }

    private fun SortField.sortDirection(): Sort.Direction =
        when (direction) {
            SortDirection.ASC -> Sort.Direction.ASC
            else -> Sort.Direction.DESC
        }

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

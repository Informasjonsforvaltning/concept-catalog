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

    fun searchConcepts(orgNumber: String, searchOperation: SearchOperation): List<BegrepDBO> =
        conceptRepository.find<BegrepDBO>(searchOperation.toMongoQuery(orgNumber))
            .doFilters(orgNumber, searchOperation)
            .sortConcepts(searchOperation.sort)

    private fun SearchOperation.toMongoQuery(orgNumber: String): Query {
        val searchCriteria = Criteria.where("ansvarligVirksomhet.id").`is`(orgNumber)

        if (!query.isNullOrBlank()) searchCriteria.orOperator(fields.queryCriteria(query))

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

private fun List<BegrepDBO>.doFilters(orgNumber: String, searchOperation: SearchOperation): List<BegrepDBO> =
    filter { it.ansvarligVirksomhet.id == orgNumber }
        .filterByPublished(searchOperation)
        .filterByStatus(searchOperation)
        .filterByAssignedUser(searchOperation)
        .filterByOriginalId(searchOperation)
        .filterBySubject(searchOperation)
        .filterByLabel(searchOperation)
        .filterByInternalFields(searchOperation)

private fun List<BegrepDBO>.filterByStatus(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.status != null) {
        filter { searchOperation.filters.status.value.contains(it.statusURI) }
    } else this

private fun List<BegrepDBO>.filterByPublished(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.published != null) {
        filter { it.erPublisert == searchOperation.filters.published.value }
    } else this

private fun List<BegrepDBO>.filterByAssignedUser(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.assignedUser != null) {
        filter { searchOperation.filters.assignedUser.value.contains(it.assignedUser) }
    } else this

private fun List<BegrepDBO>.filterByOriginalId(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.originalId != null) {
        filter { searchOperation.filters.originalId.value.contains(it.originaltBegrep) }
    } else this

private fun List<BegrepDBO>.filterByLabel(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.label != null) {
        filter {
            searchOperation.filters.label.value
                .any { label -> it.merkelapp?.contains(label) ?: false }
        }
    } else this

private fun List<BegrepDBO>.filterBySubject(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.subject != null) {
        filter {
            searchOperation.filters.subject.value
                .any { filterCode -> it.fagområdeKoder?.contains(filterCode) ?: false }
        }
    } else this

private fun List<BegrepDBO>.filterByInternalFields(searchOperation: SearchOperation): List<BegrepDBO> =
    if (searchOperation.filters.internalFields != null) {
        filter {
            searchOperation.filters.internalFields.value
                .all { (key, value) -> value.contains(it.interneFelt?.get(key)?.value) }
        }
    } else this

private fun List<BegrepDBO>.sortConcepts(sortValues: SortField): List<BegrepDBO> {
    val direction = sortValues.sortDirection()
    return when {
        sortValues.field == SortFieldEnum.ANBEFALT_TERM_NB && direction == Sort.Direction.ASC ->
            sortedBy { it.anbefaltTerm?.navn?.get("nb") }
        sortValues.field == SortFieldEnum.ANBEFALT_TERM_NB && direction == Sort.Direction.DESC ->
            sortedByDescending { it.anbefaltTerm?.navn?.get("nb") }
        direction == Sort.Direction.DESC ->
            sortedByDescending { it.endringslogelement?.endringstidspunkt }
        else -> sortedBy { it.endringslogelement?.endringstidspunkt }
    }
}

private fun SortField.sortDirection(): Sort.Direction =
    when (direction) {
        SortDirection.ASC -> Sort.Direction.ASC
        else -> Sort.Direction.DESC
    }

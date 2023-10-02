package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.*
import org.springframework.data.domain.Sort

fun resolveSearch(concepts: List<BegrepDBO>, searchOperation: SearchOperation): List<BegrepDBO> =
    concepts
        .doFilters(searchOperation)
        .doSearch(searchOperation)
        .sortConcepts(searchOperation.sort)

private fun List<BegrepDBO>.doSearch(searchOperation: SearchOperation): List<BegrepDBO> =
    if (!searchOperation.query.isNullOrBlank()) {
        filter { it.matchesSearch(searchOperation.query, searchOperation.fields) }
    } else this

private fun BegrepDBO.matchesSearch(searchQuery: String, queryFields: QueryFields): Boolean =
    when {
        queryFields.anbefaltTerm && anbefaltTerm?.navn.oneLangMatchesQuery(searchQuery) -> true
        queryFields.frarådetTerm && frarådetTerm.oneLangListValueMatchesQuery(searchQuery) -> true
        queryFields.tillattTerm && tillattTerm.oneLangListValueMatchesQuery(searchQuery) -> true
        queryFields.definisjon && definisjon?.tekst.oneLangMatchesQuery(searchQuery) -> true
        queryFields.merknad && merknad.oneLangMatchesQuery(searchQuery) -> true
        else -> false
    }

private fun Map<String, String>?.oneLangMatchesQuery(query: String): Boolean =
    if (this == null) false
    else {
        val nb = get("nb")
        val nn = get("nn")
        val en = get("en")
        when {
            nb != null && nb.lowercase().contains(query.lowercase()) -> true
            nn != null && nn.lowercase().contains(query.lowercase()) -> true
            en != null && en.lowercase().contains(query.lowercase()) -> true
            else -> false
        }
    }

private fun Map<String, List<String>>?.oneLangListValueMatchesQuery(query: String): Boolean =
    if (this == null) false
    else {
        val nb = get("nb")
        val nn = get("nn")
        val en = get("en")
        when {
            nb != null && nb.any { it.lowercase().contains(query.lowercase()) } -> true
            nn != null && nn.any { it.lowercase().contains(query.lowercase()) } -> true
            en != null && en.any { it.lowercase().contains(query.lowercase()) } -> true
            else -> false
        }
    }

private fun List<BegrepDBO>.doFilters(searchOperation: SearchOperation): List<BegrepDBO> =
    filterByPublished(searchOperation)
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

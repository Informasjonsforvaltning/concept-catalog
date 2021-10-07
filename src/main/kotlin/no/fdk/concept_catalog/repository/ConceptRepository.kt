package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.Status
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


const val matchTerm =
    "{" +
        "'ansvarligVirksomhet.id' : ?0," +
        "\$or : [" +
            "{'anbefaltTerm.navn.nb' : { \$regex: /\\b?1/, \$options: 'i' }}, " +
            "{'anbefaltTerm.navn.nn' : { \$regex: /\\b?1/, \$options: 'i' }}, " +
            "{'anbefaltTerm.navn.en' : { \$regex: /\\b?1/, \$options: 'i' }} ]" +
    "}"

@Repository
interface ConceptRepository : MongoRepository<BegrepDBO, String?> {
    fun countBegrepByAnsvarligVirksomhetId(orgNr: String): Long
    fun getBegrepByAnsvarligVirksomhetId(orgNr: String): List<BegrepDBO>
    fun getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr: String, status: Status): List<BegrepDBO>
    fun getByOriginaltBegrepAndStatus(originaltBegrep: String, status: Status): List<BegrepDBO>

    @Query(matchTerm)
    fun findByTermLike(
        @Param("id") ansvarligVirksomhetId: String,
        @Param("query") query: String
    ) : Set<BegrepDBO>
}

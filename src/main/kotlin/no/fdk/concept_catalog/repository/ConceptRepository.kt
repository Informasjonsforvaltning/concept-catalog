package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.Begrep
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
interface ConceptRepository : MongoRepository<Begrep, String?> {
    fun countBegrepByAnsvarligVirksomhetId(orgNr: String): Long
    fun getBegrepByAnsvarligVirksomhetId(orgNr: String): List<Begrep>
    fun getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr: String, status: Status): List<Begrep>
    fun getByOriginaltBegrepAndStatus(originaltBegrep: String, status: Status): List<Begrep>

    @Query(matchTerm)
    fun findByTermLike(
        @Param("id") ansvarligVirksomhetId: String,
        @Param("query") query: String
    ) : Set<Begrep>
}

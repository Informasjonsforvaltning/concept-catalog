package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.Status
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ConceptRepository : MongoRepository<BegrepDBO, String?> {
    fun countBegrepByAnsvarligVirksomhetId(orgNr: String): Long
    fun getBegrepByAnsvarligVirksomhetId(orgNr: String): List<BegrepDBO>
    fun getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr: String, status: Status): List<BegrepDBO>
    fun getByOriginaltBegrep(originaltBegrep: String): List<BegrepDBO>
    fun getByOriginaltBegrepAndErPublisert(originaltBegrep: String, erPublisert: Boolean): List<BegrepDBO>
    fun deleteByOriginaltBegrep(originaltBegrep: String)
}

package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Status
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository


@Repository
interface ConceptRepository : MongoRepository<Begrep, String?> {
    fun countBegrepByAnsvarligVirksomhetId(orgNr: String): Long
    fun getBegrepByAnsvarligVirksomhetId(orgNr: String): List<Begrep>
    fun getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr: String, status: Status): List<Begrep>
}

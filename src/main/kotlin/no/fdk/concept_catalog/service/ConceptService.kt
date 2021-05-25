package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.repository.ConceptRepository
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(ConceptService::class.java)

@Service
class ConceptService(
    private val conceptRepository: ConceptRepository,
    private val mongoOperations: MongoOperations
) {

    fun deleteConcept(concept: Begrep) =
        conceptRepository.delete(concept)

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)

    fun createConcept(concept: Begrep) {
        conceptRepository.save(concept.copy(id = null))
    }

    fun createConcepts(concepts: List<Begrep>) {
        conceptRepository.saveAll(
            concepts.map { it.copy(id = null) }
        )
    }

    fun getConceptsForOrganization(orgNr: String, status: Status?): List<Begrep> =
        if (status == null) conceptRepository.getBegrepByAnsvarligVirksomhetId(orgNr)
        else conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus(orgNr, status)

    fun statusFromString(str: String?): Status? =
        when(str?.lowercase()) {
            Status.UTKAST.value -> Status.UTKAST
            Status.GODKJENT.value -> Status.GODKJENT
            Status.PUBLISERT.value -> Status.PUBLISERT
            else -> null
        }

    fun getAllPublisherIds(): List<String> {
        return mongoOperations
            .query(Begrep::class.java)
            .distinct("ansvarligVirksomhet.id")
            .`as`(String::class.java)
            .all()
    }

}

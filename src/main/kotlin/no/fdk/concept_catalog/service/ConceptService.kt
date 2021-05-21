package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.repository.ConceptRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service


@Service
class ConceptService(private val conceptRepository: ConceptRepository) {

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)

}

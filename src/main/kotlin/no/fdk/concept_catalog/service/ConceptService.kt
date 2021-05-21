package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.repository.ConceptRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service


@Service
class ConceptService(private val conceptRepository: ConceptRepository) {

    fun getConceptById(id: String): Begrep? =
        conceptRepository.findByIdOrNull(id)

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

}

package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.model.ChangeRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChangeRequestRepository : JpaRepository<ChangeRequest, String> {
    fun findByCatalogId(catalogId: String): List<ChangeRequest>

    fun findByCatalogIdAndStatus(catalogId: String, status: ChangeRequestStatus): List<ChangeRequest>

    fun findByCatalogIdAndConceptId(catalogId: String, conceptId: String): List<ChangeRequest>

    fun findByCatalogIdAndStatusAndConceptId(catalogId: String, status: ChangeRequestStatus, conceptId: String): List<ChangeRequest>

    fun findByConceptIdAndStatus(conceptId: String, status: ChangeRequestStatus): List<ChangeRequest>

    fun findByIdAndCatalogId(id: String, catalogId: String): ChangeRequest?
}

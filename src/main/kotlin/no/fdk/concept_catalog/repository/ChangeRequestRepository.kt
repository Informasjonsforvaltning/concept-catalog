package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.ChangeRequest
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ChangeRequestRepository: MongoRepository<ChangeRequest, String>{
    fun getByCatalogId(catalogId: String): List<ChangeRequest>
    fun getByCatalogIdAndConceptId(catalogId: String, conceptId: String): List<ChangeRequest>
    fun getByIdAndCatalogId(id: String, catalogId: String): ChangeRequest?
}

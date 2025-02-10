package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.model.ImportResultStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ImportResultRepository : MongoRepository<ImportResult, String> {
    fun findFirstByStatusAndExtractionRecordsExternalId(
        importResultStatus: ImportResultStatus,
        externalId: String
    ): ImportResult?

    fun findAllByCatalogId(catalogId: String): List<ImportResult>
}

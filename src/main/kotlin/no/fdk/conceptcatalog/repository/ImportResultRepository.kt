package no.fdk.conceptcatalog.repository

import no.fdk.conceptcatalog.model.ImportResult
import no.fdk.conceptcatalog.model.ImportResultStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImportResultRepository : JpaRepository<ImportResult, String> {
    @Query(
        value = """
            SELECT * FROM import_results
            WHERE catalog_id = :catalogId
              AND status = :status
              AND concept_extractions @> jsonb_build_array(jsonb_build_object('extractionRecord', jsonb_build_object('externalId', :externalId)))
            LIMIT 1
        """,
        nativeQuery = true,
    )
    fun findFirstByCatalogIdAndStatusAndExternalId(catalogId: String, status: String, externalId: String): ImportResult?

    fun findAllByCatalogId(catalogId: String): List<ImportResult>
}

package no.fdk.concept_catalog.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

enum class ImportResultStatus { FAILED, COMPLETED }

@Document(collection = "importResult")
data class ImportResult(
    @Id
    val id: String,

    val catalogId: String,
    val status: ImportResultStatus,
    val extractionRecords: List<ExtractionRecord> = emptyList()
)

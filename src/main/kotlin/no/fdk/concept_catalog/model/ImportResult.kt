package no.fdk.concept_catalog.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

enum class ImportResultStatus { FAILED, COMPLETED }

@Document(collection = "importResults")
data class ImportResult(
    @Id
    val id: String,

    val created: LocalDateTime,
    val catalogId: String,
    val status: ImportResultStatus,
    val extractionRecords: List<ExtractionRecord> = emptyList()
)

package no.fdk.concept_catalog.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

enum class ImportResultStatus { FAILED, COMPLETED, IN_PROGRESS, PENDING_CONFIRMATION, CANCELLED }

@Document(collection = "importResults")
data class ImportResult(
    @Id
    val id: String,

    val created: LocalDateTime,
    val catalogId: String,
    val status: ImportResultStatus,
    val extractionRecords: List<ExtractionRecord> = emptyList(),
    val conceptExtraction: List<ConceptExtraction> = emptyList(),
    val totalConcepts: Int = 0,
    val extractedConcepts: Int = 0,
    )

package no.fdk.concept_catalog.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

enum class ImportResultStatus { FAILED, COMPLETED, PARTIALLY_COMPLETED, IN_PROGRESS, PENDING_CONFIRMATION, SAVING, CANCELLED }

@Document(collection = "importResults")
data class ImportResult(
    @Id
    val id: String,

    val created: LocalDateTime,
    val catalogId: String,
    val status: ImportResultStatus,
    val extractionRecords: List<ExtractionRecord> = emptyList(),
    val conceptExtractions: List<ConceptExtraction> = emptyList(),
    val totalConcepts: Int = 0,
    val extractedConcepts: Int = 0,
    val savedConcepts: Int = 0,
    val failureMessage: String? = null,
    )

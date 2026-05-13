package no.fdk.concept_catalog.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

enum class ImportResultStatus { FAILED, COMPLETED, PARTIALLY_COMPLETED, IN_PROGRESS, PENDING_CONFIRMATION, SAVING, CANCELLED }

@Entity
@Table(name = "import_results")
data class ImportResult(
    @Id
    @Column(name = "id")
    val id: String = "",

    @Column(name = "created", nullable = false)
    val created: LocalDateTime = LocalDateTime.now(),

    @Column(name = "catalog_id", nullable = false)
    val catalogId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ImportResultStatus = ImportResultStatus.IN_PROGRESS,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concept_extractions", nullable = false, columnDefinition = "jsonb")
    val conceptExtractions: List<ConceptExtraction> = emptyList(),

    @Column(name = "total_concepts")
    val totalConcepts: Int? = 0,

    @Column(name = "extracted_concepts")
    val extractedConcepts: Int? = 0,

    @Column(name = "saved_concepts")
    val savedConcepts: Int? = 0,

    @Column(name = "failure_message")
    val failureMessage: String? = null,
)

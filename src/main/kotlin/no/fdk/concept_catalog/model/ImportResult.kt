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
    val conceptExtractions: List<ConceptExtraction> = emptyList(),
    val totalConcepts: Int = 0,
    val extractedConcepts: Int = 0,
    val savedConcepts: Int = 0,
    val failureMessage: String? = null,
    )

data class ImportResultSummary(
    val id: String,
    val created: LocalDateTime,
    val status: ImportResultStatus,
    val recordsWithNoIssues: Int = 0,
    val warningIssues: Int = 0,
    val errorIssues: Int = 0,
)

val ImportResult.toImportResultSummary: ImportResultSummary get() {
        val recordsWithNoIssues = conceptExtractions.allExtractionRecords
            .filter { it.extractResult.issues.isEmpty() }.size

        val countByIssueType: (issueType: IssueType) -> Int = { issueType ->
            conceptExtractions.allExtractionRecords.map { it.extractResult.issues }
                .flatten()
                .count { it.type == issueType }
        }

        val warningIssues = countByIssueType(IssueType.WARNING)
        val errorIssues = countByIssueType(IssueType.ERROR)
        return ImportResultSummary(
            id = id,
            created = created,
            status = status,
            recordsWithNoIssues = recordsWithNoIssues,
            warningIssues = warningIssues,
            errorIssues = errorIssues
        )
    }

val Iterable<ImportResult>.toImportResultSummaries: List<ImportResultSummary>
    get() = this.map { it.toImportResultSummary }
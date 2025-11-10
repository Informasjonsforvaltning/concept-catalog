package no.fdk.concept_catalog.model

enum class ConceptExtractionStatus {
    FAILED, PENDING_CONFIRMATION, SAVING, CANCELLED, SAVING_FAILED, COMPLETED
}

data class ConceptExtraction(
    val concept: BegrepDBO,
    val extractionRecord: ExtractionRecord,
    val conceptExtractionStatus: ConceptExtractionStatus = ConceptExtractionStatus.PENDING_CONFIRMATION
)

val Iterable<ConceptExtraction>.allFailed: Boolean
    get() = all { it.conceptExtractionStatus == ConceptExtractionStatus.FAILED }

val Iterable<ConceptExtraction>.hasError: Boolean
    get() = any { it.extractionRecord.extractResult.hasError() }

val Iterable<ConceptExtraction>.allExtractionRecords: List<ExtractionRecord>
    get() = map { it.extractionRecord }
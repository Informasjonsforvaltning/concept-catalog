package no.fdk.concept_catalog.model

enum class ConceptExtractionStatus {
    PENDING_CONFIRMATION, SAVING, FAILED, COMPLETED
}

data class ConceptExtraction(
    val concept: BegrepDBO,
    val extractionRecord: ExtractionRecord,
    val conceptExtractionStatus: ConceptExtractionStatus = ConceptExtractionStatus.PENDING_CONFIRMATION
)

val Iterable<ConceptExtraction>.hasError: Boolean
    get() = any { it.extractionRecord.extractResult.hasError() }

val Iterable<ConceptExtraction>.allExtractionRecords: List<ExtractionRecord>
    get() = map { it.extractionRecord }
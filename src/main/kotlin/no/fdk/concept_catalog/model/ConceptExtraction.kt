package no.fdk.concept_catalog.model

data class ConceptExtraction(
    val concept: BegrepDBO,
    val extractionRecord: ExtractionRecord
)

val Iterable<ConceptExtraction>.hasError: Boolean
    get() = any { it.extractionRecord.extractResult.hasError() }

val Iterable<ConceptExtraction>.allExtractionRecords: List<ExtractionRecord>
    get() = map { it.extractionRecord }
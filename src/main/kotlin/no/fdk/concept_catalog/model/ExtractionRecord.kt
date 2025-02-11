package no.fdk.concept_catalog.model

data class ExtractionRecord(val internalId: String? = null, val externalId: String, val extractResult: ExtractResult)

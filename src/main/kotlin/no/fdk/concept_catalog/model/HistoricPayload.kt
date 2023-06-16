package no.fdk.concept_catalog.model

data class HistoricPayload(
    val person: User,
    val operations: List<JsonPatchOperation>
)

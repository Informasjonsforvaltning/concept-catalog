package no.fdk.conceptcatalog.model

data class HistoricPayload(val person: User, val operations: List<JsonPatchOperation>)

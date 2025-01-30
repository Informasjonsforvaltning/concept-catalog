package no.fdk.concept_catalog.model

data class ExtractResult(val operations: Set<JsonPatchOperation> = emptySet(), val issues: Set<Issue> = emptySet()) {

    fun hasError(): Boolean {
        return issues.any { it.type == IssueType.ERROR }
    }
}

fun Sequence<ExtractResult?>.merge(): ExtractResult {
    val operations = mutableSetOf<JsonPatchOperation>()
    val issues = mutableSetOf<Issue>()

    for (result in this) {
        if (result != null) {
            operations.addAll(result.operations)
            issues.addAll(result.issues)
        }
    }

    return ExtractResult(operations, issues)
}

val ExtractionRecord.allOperations: List<JsonPatchOperation>
    get() = this.extractResult.operations.toList()

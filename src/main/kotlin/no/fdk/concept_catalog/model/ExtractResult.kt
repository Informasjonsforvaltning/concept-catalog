package no.fdk.concept_catalog.model

data class ExtractResult(val operations: Set<JsonPatchOperation> = emptySet(), val issues: Set<Issue> = emptySet()) {

    fun hasError(): Boolean {
        return issues.any { it.type == IssueType.ERROR }
    }
}

fun Sequence<ExtractResult?>.merge(): ExtractResult {
    val operations = this.filterNotNull().flatMap { it.operations }.toMutableSet()

    val issues = this.filterNotNull().flatMap { it.issues }.toMutableSet()

    return ExtractResult(operations, issues)
}

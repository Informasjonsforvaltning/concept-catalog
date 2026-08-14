package no.fdk.concept_catalog.model

enum class IssueType { WARNING, ERROR }

data class Issue(val type: IssueType, val message: String)

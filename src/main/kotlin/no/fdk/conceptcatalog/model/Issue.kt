package no.fdk.conceptcatalog.model

enum class IssueType { WARNING, ERROR }

data class Issue(val type: IssueType, val message: String)

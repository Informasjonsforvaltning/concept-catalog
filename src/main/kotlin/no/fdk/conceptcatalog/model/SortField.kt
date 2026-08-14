package no.fdk.conceptcatalog.model

enum class SortFieldEnum {
    SIST_ENDRET,
    ANBEFALT_TERM,
}

enum class SortDirection {
    ASC,
    DESC,
}

class SortField(val field: SortFieldEnum, val direction: SortDirection)

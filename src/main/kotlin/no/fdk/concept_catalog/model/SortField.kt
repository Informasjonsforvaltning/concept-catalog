package no.fdk.concept_catalog.model

enum class SortFieldEnum(val label: String) {
    SIST_ENDRET("sistEndret"),
    ANBEFALT_TERM_NB("anbefaltTerm.nb"),
}

enum class SortDirection(val label: String) {
    ASC("ASC"),
    DESC("DESC"),
}

class SortField(
    val field: SortFieldEnum = SortFieldEnum.SIST_ENDRET,
    val direction: SortDirection = SortDirection.DESC,
)

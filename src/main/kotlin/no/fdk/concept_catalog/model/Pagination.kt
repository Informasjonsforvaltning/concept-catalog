package no.fdk.concept_catalog.model

data class Pagination (
    val page: Int = 0,
    val size: Int = 10
)

data class Paginated (
    val hits: List<Begrep>,
    val page: PageMeta
)

data class PageMeta (
    val currentPage: Int,
    val size: Int,
    val totalElements: Int,
    val totalPages: Int
)

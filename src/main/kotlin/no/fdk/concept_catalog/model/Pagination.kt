package no.fdk.concept_catalog.model

data class Pagination (
    private val page: Int = 0,
    private val size: Int = 10
) {
    fun getPage(): Int = page.let { if (it < 0) 0 else it }
    fun getSize(): Int = size.let { if (it < 1) 10 else it }
}

data class Paginated (
    val hits: List<Begrep>,
    val page: PageMeta
)

data class PageMeta (
    val currentPage: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Long
)

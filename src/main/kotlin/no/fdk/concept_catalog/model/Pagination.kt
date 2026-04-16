package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonProperty

data class Pagination (
    @param:JsonProperty("page", required = false)
    private val page: Int? = null,
    @param:JsonProperty("size", required = false)
    private val size: Int? = null
) {
    fun getPage(): Int = page.let { if (it == null || it < 0) 0 else it }
    fun getSize(): Int = size.let { if (it == null || it < 1) 10 else it }
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

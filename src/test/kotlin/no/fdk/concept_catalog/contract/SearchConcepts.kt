package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class SearchConcepts : ContractTestsBase() {
    val sortByModified = SortField(field = SortFieldEnum.SIST_ENDRET, direction = SortDirection.DESC)

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("test")),
            null,
            HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val response = authorizedRequest(
            "/begreper/search?orgNummer=999888777",
            mapper.writeValueAsString(SearchOperation("test")), JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Ok for read access`() {
        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("test")), JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(), result.hits)
    }

    @Test
    fun `Query returns correct results`() {
        addToElasticsearchIndex(listOf(CurrentConcept(BEGREP_1.toDBO()), CurrentConcept(BEGREP_2.toDBO())))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("Begrep", sort = sortByModified)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1, BEGREP_2), result.hits)
    }

    @Test
    fun `Query returns correct results when searching in definisjon`() {
        addToElasticsearchIndex(CurrentConcept(BEGREP_1.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("SEARCHABLE")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Query with status filter returns correct results`() {
        addToElasticsearchIndex(CurrentConcept(BEGREP_1.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(
                SearchOperation(
                    "Begrep",
                    filters = SearchFilters(status = SearchFilter(listOf("http://publications.europa.eu/resource/authority/concept-status/CURRENT")))
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Query with assignedUser filter returns correct results`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        addToElasticsearchIndex(CurrentConcept(BEGREP_0.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(
                SearchOperation(
                    "", filters = SearchFilters(
                        assignedUser = SearchFilter(
                            listOf("user-id")
                        )
                    )
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_0), result.hits)
    }

    @Test
    fun `Query with originalId filter returns correct results`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_1.toDBO()))

        addToElasticsearchIndex(listOf(CurrentConcept(BEGREP_0.toDBO()), CurrentConcept(BEGREP_1.toDBO())))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(
                SearchOperation(
                    "", sort = sortByModified, filters = SearchFilters(
                        originalId = SearchFilter(
                            listOf("id0-old", "id1")
                        )
                    )
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1, BEGREP_0), result.hits)
    }

    @Test
    fun `Query with published filter returns correct results`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_1.toDBO(), BEGREP_2.toDBO()))

        addToElasticsearchIndex(
            listOf(
                CurrentConcept(BEGREP_0.toDBO()),
                CurrentConcept(BEGREP_1.toDBO()),
                CurrentConcept(BEGREP_2.toDBO())
            )
        )

        val unPublishedResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(
                SearchOperation(
                    "",
                    sort = sortByModified,
                    filters = SearchFilters(published = BooleanFilter(false))
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        val publishedResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(
                SearchOperation(
                    "",
                    sort = sortByModified,
                    filters = SearchFilters(published = BooleanFilter(true))
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, publishedResponse.statusCode)
        assertEquals(HttpStatus.OK, unPublishedResponse.statusCode)

        val unPublished: Paginated = mapper.readValue(unPublishedResponse.body as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), unPublished.hits)

        val published: Paginated = mapper.readValue(publishedResponse.body as String)
        assertEquals(listOf(BEGREP_0), published.hits)
    }

    @Test
    fun `Query with subjects filter returns correct results`() {
        mongoOperations.insertAll(listOf(BEGREP_4.toDBO(), BEGREP_5.toDBO()))

        addToElasticsearchIndex(listOf(CurrentConcept(BEGREP_4.toDBO()), CurrentConcept(BEGREP_5.toDBO())))

        val withSubjectFagomr1Response = authorizedRequest(
            "/begreper/search?orgNummer=111222333",

            mapper.writeValueAsString(
                SearchOperation(
                    "",
                    filters = SearchFilters(subject = SearchFilter(listOf("5e6b2561-6157-4eb4-b396-d773cd00de12")))
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        val withSubjectFagomr3Response = authorizedRequest(
            "/begreper/search?orgNummer=111222333",

            mapper.writeValueAsString(
                SearchOperation(
                    "",
                    filters = SearchFilters(
                        subject = SearchFilter(
                            listOf(
                                "5e6b2561-6157-4eb4-b396-d773cd00de12",
                                "fagomr3"
                            )
                        )
                    )
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, withSubjectFagomr1Response.statusCode)
        assertEquals(HttpStatus.OK, withSubjectFagomr3Response.statusCode)

        val withSubjectFagomr1: Paginated = mapper.readValue(withSubjectFagomr1Response.body as String)
        assertEquals(listOf(BEGREP_5), withSubjectFagomr1.hits)

        val withSubjectFagomr3: Paginated = mapper.readValue(withSubjectFagomr3Response.body as String)
        assertEquals(listOf(BEGREP_4, BEGREP_5), withSubjectFagomr3.hits)
    }

    @Test
    fun `Query with internalFields filter returns correct results`() {
        mongoOperations.insert(BEGREP_4.toDBO())

        addToElasticsearchIndex(CurrentConcept(BEGREP_4.toDBO()))

        val withInternalFieldsResponse = authorizedRequest(
            "/begreper/search?orgNummer=111222333",
            mapper.writeValueAsString(
                SearchOperation(
                    "", filters = SearchFilters(
                        internalFields =
                            SearchFilter(mapOf(Pair("felt1", listOf("true")), Pair("felt2", listOf("false"))))
                    )
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        val withoutInternalFieldsResponse = authorizedRequest(
            "/begreper/search?orgNummer=111222333",
            mapper.writeValueAsString(
                SearchOperation(
                    "", filters = SearchFilters(
                        internalFields =
                            SearchFilter(mapOf(Pair("felt1", listOf("true")), Pair("felt2", listOf("true"))))
                    )
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, withInternalFieldsResponse.statusCode)
        assertEquals(HttpStatus.OK, withoutInternalFieldsResponse.statusCode)

        val withInternalFields: Paginated = mapper.readValue(withInternalFieldsResponse.body as String)
        assertEquals(listOf(BEGREP_4), withInternalFields.hits)

        val withoutInternalFields: Paginated = mapper.readValue(withoutInternalFieldsResponse.body as String)
        assertEquals(emptyList(), withoutInternalFields.hits)
    }

    @Test
    fun `Query with label filter returns correct results`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        addToElasticsearchIndex(CurrentConcept(BEGREP_0.toDBO()))

        val withLabelResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(
                SearchOperation(
                    "", filters = SearchFilters(
                        label =
                            SearchFilter(listOf("merkelapp1"))
                    )
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        val withoutLabelResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(
                SearchOperation(
                    "", filters = SearchFilters(
                        label =
                            SearchFilter(listOf("merkelapp3"))
                    )
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, withLabelResponse.statusCode)
        assertEquals(HttpStatus.OK, withoutLabelResponse.statusCode)

        val withInternalFields: Paginated = mapper.readValue(withLabelResponse.body as String)
        assertEquals(listOf(BEGREP_0), withInternalFields.hits)

        val withoutInternalFields: Paginated = mapper.readValue(withoutLabelResponse.body as String)
        assertEquals(emptyList(), withoutInternalFields.hits)
    }

    @Test
    fun `Query filter with several values returns correct results`() {
        addToElasticsearchIndex(listOf(CurrentConcept(BEGREP_1.toDBO()), CurrentConcept(BEGREP_2.toDBO())))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(
                SearchOperation(
                    "Begrep", sort = sortByModified,
                    filters = SearchFilters(
                        status = SearchFilter(
                            listOf(
                                "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
                                "http://publications.europa.eu/resource/authority/concept-status/CANDIDATE"
                            )
                        )
                    )
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1, BEGREP_2), result.hits)
    }

    @Test
    fun `Query returns correct results when only title is active`() {
        addToElasticsearchIndex(listOf(CurrentConcept(BEGREP_1.toDBO()), CurrentConcept(BEGREP_2.toDBO())))

        val queryFields = QueryFields(definisjon = false, merknad = false, frarådetTerm = false, tillattTerm = false)

        val titleResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(SearchOperation("Begrep", sort = sortByModified, fields = queryFields)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, titleResponse.statusCode)

        val titleResult: Paginated = mapper.readValue(titleResponse.body as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), titleResult.hits)

        val descriptionResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(SearchOperation("searchable", fields = queryFields)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, descriptionResponse.statusCode)

        val descriptionResult: Paginated = mapper.readValue(descriptionResponse.body as String)
        assertEquals(emptyList(), descriptionResult.hits)

        val statusResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(
                SearchOperation(
                    query = "Begrep", fields = queryFields,
                    filters = SearchFilters(status = SearchFilter(listOf("http://publications.europa.eu/resource/authority/concept-status/CANDIDATE")))
                )
            ), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, statusResponse.statusCode)

        val statusResult: Paginated = mapper.readValue(statusResponse.body as String)

        assertEquals(listOf(BEGREP_2), statusResult.hits)
    }

    @Test
    fun `Empty query returns all current versions`() {
        mongoOperations.insertAll(listOf(BEGREP_1.toDBO(), BEGREP_0.toDBO()))

        addToElasticsearchIndex(
            listOf(
                CurrentConcept(BEGREP_0.toDBO()),
                CurrentConcept(BEGREP_1.toDBO()),
                CurrentConcept(BEGREP_2.toDBO())
            )
        )

        val queryFields =
            QueryFields(definisjon = false, anbefaltTerm = false, frarådetTerm = false, tillattTerm = false)
        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(SearchOperation("", sort = sortByModified, fields = queryFields)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val titleResult: Paginated = mapper.readValue(response.body as String)
        assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2), titleResult.hits)
    }

    @Test
    fun `Query returns correct results when searching in tillattTerm`() {
        addToElasticsearchIndex(CurrentConcept(BEGREP_2.toDBO()))

        val queryFields = QueryFields(definisjon = false, merknad = false, frarådetTerm = false, anbefaltTerm = false)

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(SearchOperation("Lorem ipsum", fields = queryFields)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_2), result.hits)
    }

    @Test
    fun `Query returns correct results when searching in merknad`() {
        addToElasticsearchIndex(CurrentConcept(BEGREP_1.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("asdf")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Query returns correct results when searching in terms`() {
        mongoOperations.insertAll(listOf(BEGREP_1.toDBO(), BEGREP_0.toDBO()))

        addToElasticsearchIndex(
            listOf(
                CurrentConcept(BEGREP_0.toDBO()),
                CurrentConcept(BEGREP_1.toDBO()),
                CurrentConcept(BEGREP_2.toDBO())
            )
        )

        val queryFields = QueryFields(definisjon = false, merknad = false)

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("Lorem ipsum", fields = queryFields)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2).sortedBy { it.id }, result.hits.sortedBy { it.id })
    }

    @Test
    fun `Status filter returns correct results`() {
        addToElasticsearchIndex(CurrentConcept(BEGREP_2.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(
                SearchOperation(
                    "",
                    filters = SearchFilters(status = SearchFilter(listOf("http://publications.europa.eu/resource/authority/concept-status/CANDIDATE")))
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_2), result.hits)
    }

    @Test
    fun `Query with current version filter returns correct results`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        addToElasticsearchIndex(CurrentConcept(BEGREP_0.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("definisjon")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_0), result.hits)
    }

    @Test
    fun `Query returns no results`() {
        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(SearchOperation("zxcvbnm")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(), result.hits)
    }

    @Nested
    inner class Paginate {

        @Test
        fun `Paginate handles invalid values`() {
            mongoOperations.insertAll(listOf(BEGREP_1.toDBO(), BEGREP_0.toDBO()))

            addToElasticsearchIndex(
                listOf(
                    CurrentConcept(BEGREP_0.toDBO()),
                    CurrentConcept(BEGREP_1.toDBO()),
                    CurrentConcept(BEGREP_2.toDBO())
                )
            )

            val response = authorizedRequest(
                "/begreper/search?orgNummer=123456789",

                mapper.writeValueAsString(
                    SearchOperation(
                        "",
                        sort = sortByModified,
                        pagination = Pagination(page = -1, size = -1)
                    )
                ),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK, response.statusCode)

            val result: Paginated = mapper.readValue(response.body as String)

            assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2), result.hits)
        }

        @Test
        fun `Empty list when exceeding actual page count`() {
            val response = authorizedRequest(
                "/begreper/search?orgNummer=123456789",

                mapper.writeValueAsString(SearchOperation("", pagination = Pagination(page = 99, size = 10))),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK, response.statusCode)

            val result: Paginated = mapper.readValue(response.body as String)

            assertEquals(emptyList(), result.hits)
        }

        @Test
        fun `Pages handled correctly`() {
            mongoOperations.insertAll(listOf(BEGREP_1.toDBO(), BEGREP_0.toDBO()))

            addToElasticsearchIndex(
                listOf(
                    CurrentConcept(BEGREP_0.toDBO()),
                    CurrentConcept(BEGREP_1.toDBO()),
                    CurrentConcept(BEGREP_2.toDBO())
                )
            )

            val firstResponse = authorizedRequest(
                "/begreper/search?orgNummer=123456789",

                mapper.writeValueAsString(
                    SearchOperation(
                        "",
                        sort = sortByModified,
                        pagination = Pagination(page = 0, size = 2)
                    )
                ),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            val secondResponse = authorizedRequest(
                "/begreper/search?orgNummer=123456789",

                mapper.writeValueAsString(
                    SearchOperation(
                        "",
                        sort = sortByModified,
                        pagination = Pagination(page = 1, size = 2)
                    )
                ),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK, firstResponse.statusCode)
            assertEquals(HttpStatus.OK, secondResponse.statusCode)

            val result0: Paginated = mapper.readValue(firstResponse.body as String)
            val result1: Paginated = mapper.readValue(secondResponse.body as String)

            assertEquals(listOf(BEGREP_1, BEGREP_0), result0.hits)
            assertEquals(listOf(BEGREP_2), result1.hits)
        }
    }

    @Test
    fun `Query returns sorted results ordered by sistEndret ascending`() {
        mongoOperations.insertAll(listOf(BEGREP_1.toDBO(), BEGREP_0.toDBO()))

        addToElasticsearchIndex(
            listOf(
                CurrentConcept(BEGREP_0.toDBO()),
                CurrentConcept(BEGREP_1.toDBO()),
                CurrentConcept(BEGREP_2.toDBO())
            )
        )

        val searchOp = SearchOperation(
            query = "",
            sort = SortField(field = SortFieldEnum.SIST_ENDRET, direction = SortDirection.ASC)
        )

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_2, BEGREP_0, BEGREP_1), result.hits)
    }

    @Test
    fun `Query returns sorted results ordered by anbefaltTerm ascending`() {
        addToElasticsearchIndex(
            listOf(
                CurrentConcept(BEGREP_0.toDBO()),
                CurrentConcept(BEGREP_1.toDBO()),
                CurrentConcept(BEGREP_2.toDBO())
            )
        )

        val searchOp = SearchOperation(
            query = "",
            sort = SortField(field = SortFieldEnum.ANBEFALT_TERM, direction = SortDirection.ASC)
        )

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(BEGREP_0.id, result.hits[0].id)
        assertEquals(BEGREP_1.id, result.hits[1].id)
        assertEquals(BEGREP_2.id, result.hits[2].id)
    }

    @Test
    fun `Query returns sorted results ordered by anbefaltTerm descending`() {
        addToElasticsearchIndex(
            listOf(
                CurrentConcept(BEGREP_0.toDBO()),
                CurrentConcept(BEGREP_1.toDBO()),
                CurrentConcept(BEGREP_2.toDBO())
            )
        )

        val searchOp = SearchOperation(
            query = "",
            sort = SortField(field = SortFieldEnum.ANBEFALT_TERM, direction = SortDirection.DESC)
        )

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(BEGREP_2.id, result.hits[0].id)
        assertEquals(BEGREP_1.id, result.hits[1].id)
        assertEquals(BEGREP_0.id, result.hits[2].id)
    }

    @Test
    fun `Combination of status and published filter returns correct results`() {
        addToElasticsearchIndex(CurrentConcept(BEGREP_1.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(
                SearchOperation(
                    query = "",
                    filters = SearchFilters(
                        status = SearchFilter(listOf("http://publications.europa.eu/resource/authority/concept-status/CURRENT")),
                        published = BooleanFilter(false)
                    )
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Handle concepts with multiple unpublished revisions`() {
        mongoOperations.insert(BEGREP_HAS_MULTIPLE_REVISIONS.toDBO())

        addToElasticsearchIndex(CurrentConcept(BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND.toDBO()))

        val response = authorizedRequest(
            "/begreper/search?orgNummer=222222222",

            mapper.writeValueAsString(
                SearchOperation(
                    query = ""
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Paginated = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_UNPUBLISHED_REVISION_MULTIPLE_SECOND), result.hits)
    }
}

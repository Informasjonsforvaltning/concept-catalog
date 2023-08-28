package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_0_OLD
import no.fdk.concept_catalog.utils.BEGREP_1
import no.fdk.concept_catalog.utils.BEGREP_2
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

private val mapper = JacksonConfigurer().objectMapper()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class SearchConcepts : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest("/begreper/search?orgNummer=123456789", port, mapper.writeValueAsString(SearchOperation("test")), null, HttpMethod.POST)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=999888777",
            port, mapper.writeValueAsString(SearchOperation("test")), JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok for read access`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("test")), JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(), result.hits)
    }

    @Test
    fun `Query returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), result.hits)

    }

    @Test
    fun `Query returns correct results when searching in definisjon`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("ABLE")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result.hits)

    }

    @Test
    fun `Query with status filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep", filters = SearchFilters(status = SearchFilter(listOf("godkjent"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Query with assignedUser filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(assignedUser = SearchFilter(
                listOf("user-id")
            )))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0), result.hits)
    }

    @Test
    fun `Query with originalId filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(originalId = SearchFilter(
                listOf("id0-old", "id1")
            )))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_0), result.hits)
    }

    @Test
    fun `Query with published filter returns correct results`() {
        val unPublishedResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(published = BooleanFilter(false)))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        val publishedResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(published = BooleanFilter(true)))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), publishedResponse["status"])
        assertEquals(HttpStatus.OK.value(), unPublishedResponse["status"])

        val unPublished: Paginated = mapper.readValue(unPublishedResponse["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), unPublished.hits)

        val published: Paginated = mapper.readValue(publishedResponse["body"] as String)
        assertEquals(listOf(BEGREP_0), published.hits)
    }

    @Test
    fun `Query with subjects filter returns correct results`() {
        val withSubjectFagomr1Response = authorizedRequest(
            "/begreper/search?orgNummer=111222333",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(subject = SearchFilter(listOf("fagomr1"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        val withSubjectFagomr3Response = authorizedRequest(
            "/begreper/search?orgNummer=111222333",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(subject = SearchFilter(listOf("fagomr1", "fagomr3"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), withSubjectFagomr1Response["status"])
        assertEquals(HttpStatus.OK.value(), withSubjectFagomr3Response["status"])

        val withSubjectFagomr1: Paginated = mapper.readValue(withSubjectFagomr1Response["body"] as String)
        assertEquals(listOf(BEGREP_4, BEGREP_5), withSubjectFagomr1.hits)

        val withSubjectFagomr3: Paginated = mapper.readValue(withSubjectFagomr3Response["body"] as String)
        assertEquals(listOf(BEGREP_4), withSubjectFagomr3.hits)
    }
    @Test
    fun `Query with internalFields filter returns correct results`() {
        val withInternalFieldsResponse = authorizedRequest(
            "/begreper/search?orgNummer=111222333",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(internalFields =
                SearchFilter(mapOf(Pair("felt1", listOf("true")), Pair("felt2", listOf("false"))))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        val withoutInternalFieldsResponse = authorizedRequest(
            "/begreper/search?orgNummer=111222333",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(internalFields =
                SearchFilter(mapOf(Pair("felt1", listOf("true")), Pair("felt2", listOf("true"))))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), withInternalFieldsResponse["status"])
        assertEquals(HttpStatus.OK.value(), withoutInternalFieldsResponse["status"])

        val withInternalFields: Paginated = mapper.readValue(withInternalFieldsResponse["body"] as String)
        assertEquals(listOf(BEGREP_4), withInternalFields.hits)

        val withoutInternalFields: Paginated = mapper.readValue(withoutInternalFieldsResponse["body"] as String)
        assertEquals(emptyList(), withoutInternalFields.hits)
    }
    @Test
    fun `Query filter with several values returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep", filters = SearchFilters(status = SearchFilter(listOf("godkjent", "høring"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), result.hits)
    }

    @Test
    fun `Query returns correct results when only title is active`() {
        val queryFields = QueryFields(definisjon = false, merknad = false, frarådetTerm = false, tillattTerm = false)
        val titleResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep", fields = queryFields)), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), titleResponse["status"])

        val titleResult: Paginated = mapper.readValue(titleResponse["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), titleResult.hits)

        val descriptionResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("able", fields = queryFields)), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), descriptionResponse["status"])

        val descriptionResult: Paginated = mapper.readValue(descriptionResponse["body"] as String)
        assertEquals(emptyList(), descriptionResult.hits)

        val statusResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation(
                query = "Begrep", fields = queryFields,
                filters = SearchFilters(status = SearchFilter(listOf("godkjent"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), statusResponse["status"])

        val statusResult: Paginated = mapper.readValue(statusResponse["body"] as String)
        assertEquals(listOf(BEGREP_1), statusResult.hits)
    }

    @Test
    fun `Empty query returns all current versions`() {
        val queryFields = QueryFields(definisjon = false, anbefaltTerm = false, frarådetTerm = false, tillattTerm = false)
        val response = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", fields = queryFields)), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val titleResult: Paginated = mapper.readValue(response["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2), titleResult.hits)
    }

    @Test
    fun `Query returns correct results when searching in tillattTerm`() {
        val queryFields = QueryFields(definisjon = false, merknad = false, frarådetTerm = false, anbefaltTerm = false)
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Lorem ipsum", fields = queryFields)), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated= mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_2), result.hits)

    }

    @Test
    fun `Query returns correct results when searching in merknad`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("merknad")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0), result.hits)

    }

    @Test
    fun `Query returns correct results when searching in terms`() {
        val queryFields = QueryFields(definisjon = false, merknad = false)
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Lorem ipsum", fields = queryFields)), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2), result.hits)
    }

    @Test
    fun `Status filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(status = SearchFilter(listOf("godkjent"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Query with current version filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("definisjon")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0), result.hits)
    }

    @Test
    fun `Query with false value for current version filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("definisjon", filters = SearchFilters(onlyCurrentVersions = false))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0, BEGREP_0_OLD), result.hits)
    }

    @Test
    fun `Query returns no results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("zxcvbnm")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(), result.hits)
    }

    @Nested
    internal inner class Paginate {
        @Test
        fun `Paginate handles invalid values`() {
            val rsp = authorizedRequest(
                "/begreper/search?orgNummer=123456789",
                port, mapper.writeValueAsString(SearchOperation("", pagination = Pagination(page = -1, size = -1))), JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val result: Paginated = mapper.readValue(rsp["body"] as String)
            assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2), result.hits)
        }

        @Test
        fun `Empty list when exceeding actual page count`() {
            val rsp = authorizedRequest(
                "/begreper/search?orgNummer=123456789",
                port, mapper.writeValueAsString(SearchOperation("", pagination = Pagination(page = 99, size = 10))), JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK.value(), rsp["status"])

            val result: Paginated = mapper.readValue(rsp["body"] as String)
            assertEquals(emptyList(), result.hits)
        }

        @Test
        fun `Pages handled correctly`() {
            val rsp0 = authorizedRequest(
                "/begreper/search?orgNummer=123456789",
                port, mapper.writeValueAsString(SearchOperation("", pagination = Pagination(page = 0, size = 2))), JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )
            val rsp1 = authorizedRequest(
                "/begreper/search?orgNummer=123456789",
                port, mapper.writeValueAsString(SearchOperation("", pagination = Pagination(page = 1, size = 2))), JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST
            )

            assertEquals(HttpStatus.OK.value(), rsp0["status"])
            assertEquals(HttpStatus.OK.value(), rsp1["status"])

            val result0: Paginated = mapper.readValue(rsp0["body"] as String)
            val result1: Paginated = mapper.readValue(rsp1["body"] as String)
            assertEquals(listOf(BEGREP_1, BEGREP_0), result0.hits)
            assertEquals(listOf(BEGREP_2), result1.hits)
        }
    }

    @Test
    fun `Query returns sorted results ordered by sistEndret ascending`() {
        val searchOp = SearchOperation(
            query = "",
            filters = SearchFilters(onlyCurrentVersions = false),
            sort = SortField(field=SortFieldEnum.SIST_ENDRET, direction=SortDirection.ASC)
        )
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0_OLD, BEGREP_2, BEGREP_0, BEGREP_1), result.hits)
    }

    @Test
    fun `Query returns sorted results ordered by anbefaltTerm descending`() {
        val searchOp = SearchOperation(
            query = "",
            filters = SearchFilters(onlyCurrentVersions = false),
            sort = SortField(field=SortFieldEnum.ANBEFALT_TERM_NB, direction=SortDirection.DESC)
        )
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0_OLD, BEGREP_0, BEGREP_2, BEGREP_1), result.hits)
    }

    @Test
    fun `Combination of status and published filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port,
            mapper.writeValueAsString(
                SearchOperation(
                    query = "",
                    filters = SearchFilters(
                        status = SearchFilter(listOf("godkjent")),
                        published = BooleanFilter(false))
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result.hits)
    }

    @Test
    fun `Handle concepts with multiple unpublished revisions`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=222222222",
            port,
            mapper.writeValueAsString(
                SearchOperation(
                    query = ""
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Paginated = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_HAS_MULTIPLE_REVISIONS), result.hits)
    }
}

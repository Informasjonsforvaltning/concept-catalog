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

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(), result)
    }

    @Test
    fun `Query returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), result)

    }

    @Test
    fun `Query returns correct results when searching in definisjon`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("ABLE")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result)

    }

    @Test
    fun `Query with status filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep", filters = SearchFilters(SearchFilter(listOf("godkjent"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result)
    }

    @Test
    fun `Query filter with several values returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("Begrep", filters = SearchFilters(SearchFilter(listOf("godkjent", "høring"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), result)
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

        val titleResult: List<Begrep> = mapper.readValue(titleResponse["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), titleResult)

        val descriptionResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("able", fields = queryFields)), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), descriptionResponse["status"])

        val descriptionResult: List<Begrep> = mapper.readValue(descriptionResponse["body"] as String)
        assertEquals(emptyList(), descriptionResult)

        val statusResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation(
                query = "Begrep", fields = queryFields,
                filters = SearchFilters(SearchFilter(listOf("godkjent"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), statusResponse["status"])

        val statusResult: List<Begrep> = mapper.readValue(statusResponse["body"] as String)
        assertEquals(listOf(BEGREP_1), statusResult)
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

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_2), result)

    }

    @Test
    fun `Query returns correct results when searching in merknad`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("merknad")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0, BEGREP_0_OLD), result)

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

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_0, BEGREP_2), result)

    }

    @Test
    fun `Status filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("", filters = SearchFilters(SearchFilter(listOf("godkjent"))))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1), result)
    }

    @Test
    fun `Query with current version filter returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("definisjon", filters = SearchFilters(onlyCurrentVersions = true))), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0), result)
    }

    @Test
    fun `Query returns no results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(SearchOperation("zxcvbnm")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(), result)

    }

    @Test
    fun `Query returns sorted results ordered by sistEndret ascending`() {
        val searchOp = SearchOperation(
            query = "",
            sort = SortField(field=SortFieldEnum.SIST_ENDRET, direction=SortDirection.ASC)
        )
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0_OLD, BEGREP_2, BEGREP_0, BEGREP_1), result)
    }

    @Test
    fun `Query returns sorted results ordered by anbefaltTerm descending`() {
        val searchOp = SearchOperation(
            query = "",
            sort = SortField(field=SortFieldEnum.ANBEFALT_TERM_NB, direction=SortDirection.DESC)
        )
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(searchOp), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0_OLD, BEGREP_0, BEGREP_2, BEGREP_1), result)
    }
}

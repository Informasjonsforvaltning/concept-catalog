package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.JsonSearchOperation
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

private val mapper = jacksonObjectMapper()

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
        val rsp = authorizedRequest("/begreper/search?orgNummer=123456789", port, mapper.writeValueAsString(JsonSearchOperation("test")), null, HttpMethod.POST)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=999888777",
            port, mapper.writeValueAsString(JsonSearchOperation("test")), JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok for read access`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(JsonSearchOperation("test")), JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(), result)
    }

    @Test
    fun `Empty query returns bad request`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(JsonSearchOperation("")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])

    }

    @Test
    fun `Query returns correct results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(JsonSearchOperation("Begrep")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_1, BEGREP_2), result)

    }

    @Test
    fun `Query returns no results`() {
        val rsp = authorizedRequest(
            "/begreper/search?orgNummer=123456789",
            port, mapper.writeValueAsString(JsonSearchOperation("zxcvbnm")), JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(), result)

    }


}

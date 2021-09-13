package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Begrep
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
class GetConcepts : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest("/begreper?orgNummer=123456789", port, null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val rsp = authorizedRequest(
            "/begreper?orgNummer=999888777",
            port, null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok for read access`() {
        val rsp = authorizedRequest(
            "/begreper?orgNummer=123456789",
            port, null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD), result)
    }

    @Test
    fun `Ok for write access`() {
        val rsp = authorizedRequest(
            "/begreper?orgNummer=123456789",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrep> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD), result)

    }

    @Test
    fun `Ok for specific status`() {
        val rspUtkast = authorizedRequest(
            "/begreper?orgNummer=123456789&status=utKASt",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val rspGodkjent = authorizedRequest(
            "/begreper?orgNummer=123456789&status=GODKJENT",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val rspPublisert = authorizedRequest(
            "/begreper?orgNummer=123456789&status=publisert",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), rspUtkast["status"])
        assertEquals(HttpStatus.OK.value(), rspGodkjent["status"])
        assertEquals(HttpStatus.OK.value(), rspPublisert["status"])

        val resultUtkast: List<Begrep> = mapper.readValue(rspUtkast["body"] as String)
        val resultGodkjent: List<Begrep> = mapper.readValue(rspGodkjent["body"] as String)
        val resultPublisert: List<Begrep> = mapper.readValue(rspPublisert["body"] as String)

        assertEquals(listOf(BEGREP_0, BEGREP_0_OLD), resultPublisert)
        assertEquals(listOf(BEGREP_1), resultGodkjent)
        assertEquals(listOf(BEGREP_2), resultUtkast)
    }

}

package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_WRONG_ORG
import no.fdk.concept_catalog.utils.authorizedRequest
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
class CreateConcept : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest("/begreper", port, mapper.writeValueAsString(BEGREP_0), null, HttpMethod.POST)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val rsp = authorizedRequest(
            "/begreper",
            port,
            mapper.writeValueAsString(BEGREP_0),
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Forbidden when concept has non write access orgId`() {
        val rsp = authorizedRequest(
            "/begreper",
            port,
            mapper.writeValueAsString(BEGREP_WRONG_ORG),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok - Created - for write access`() {
        val rsp = authorizedRequest(
            "/begreper",
            port,
            mapper.writeValueAsString(BEGREP_0),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.CREATED.value(), rsp["status"])
    }

}

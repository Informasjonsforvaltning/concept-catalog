package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class DeleteConcept : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest("/begreper/id0", port, null, null, HttpMethod.DELETE)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val rsp =
            authorizedRequest("/begreper/id0", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.DELETE)

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok - No content - for write access`() {
        val rsp =
            authorizedRequest("/begreper/id0", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.DELETE)

        assertEquals(HttpStatus.NO_CONTENT.value(), rsp["status"])
    }

}
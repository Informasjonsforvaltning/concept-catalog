package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Suggestion
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_1
import no.fdk.concept_catalog.utils.BEGREP_2
import no.fdk.concept_catalog.utils.authorizedRequest
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class SuggestConcepts : ApiTestContext() {
    private val mapper = JacksonConfigurer().objectMapper()

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest(
            "/begreper/suggestions?org=123456789&q=test",
            port,
            null,
            null,
            HttpMethod.GET
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val rsp = authorizedRequest(
            "/begreper/suggestions?org=999888777&q=test",
            port,
            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok for read access`() {
        val rsp = authorizedRequest(
            "/begreper/suggestions?org=123456789&q=begr",
            port, null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val expected = listOf(
            Suggestion(
                id = BEGREP_2.id!!,
                originaltBegrep = BEGREP_2.originaltBegrep!!,
                erPublisert = BEGREP_2.erPublisert,
                anbefaltTerm = BEGREP_2.anbefaltTerm,
                definisjon = BEGREP_2.definisjon?.copy(kildebeskrivelse = null)),
            Suggestion(
                id = BEGREP_1.id!!,
                originaltBegrep = BEGREP_1.originaltBegrep!!,
                erPublisert = BEGREP_1.erPublisert,
                anbefaltTerm = BEGREP_1.anbefaltTerm,
                definisjon = BEGREP_1.definisjon?.copy(kildebeskrivelse = null)
            )
        )

        val result: List<Suggestion> = mapper.readValue(rsp["body"] as String)
        assertEquals(expected, result)
    }

    @Test
    fun `Ok for write access`() {
        val rsp = authorizedRequest(
            "/begreper/suggestions?org=123456789&q=lorem",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val expected = listOf(
            Suggestion(
                id = BEGREP_1.id!!,
                originaltBegrep = BEGREP_1.originaltBegrep!!,
                erPublisert = BEGREP_1.erPublisert,
                anbefaltTerm = BEGREP_1.anbefaltTerm,
                definisjon = BEGREP_1.definisjon?.copy(kildebeskrivelse = null)
            )
        )

        val result: List<Suggestion> = mapper.readValue(rsp["body"] as String)
        assertEquals(expected, result)
    }

}

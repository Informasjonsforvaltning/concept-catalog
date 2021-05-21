package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Virksomhet
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
class CreateConcepts : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            null, HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val rsp = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Forbidden when at least one concept has non write access orgId`() {
        val rsp = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_WRONG_ORG, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok - Created - for write access`() {
        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_TO_BE_CREATED.ansvarligVirksomhet?.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val rsp = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )
        assertEquals(HttpStatus.CREATED.value(), rsp["status"])

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_TO_BE_CREATED.ansvarligVirksomhet?.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> = mapper.readValue(before["body"] as String)
        val afterList: List<Begrep> = mapper.readValue(after["body"] as String)
        assertEquals(beforeList.size + 2, afterList.size)
    }

}

package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_CREATED
import no.fdk.concept_catalog.utils.BEGREP_WRONG_ORG
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class CreateConcepts : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            null, HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val response = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Forbidden when at least one concept has non write access orgId`() {
        val response = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_WRONG_ORG, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Ok - Created - for write access`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_TO_BE_CREATED.ansvarligVirksomhet.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val response = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.CREATED.value(), response["status"])

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_TO_BE_CREATED.ansvarligVirksomhet.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> = mapper.readValue(before["body"] as String)
        val afterList: List<Begrep> = mapper.readValue(after["body"] as String)

        assertEquals(beforeList.size + 2, afterList.size)
    }

    @Test
    fun `Bad request - Invalid version - for write access`() {
        val response = authorizedRequest(
            "/begreper/import", port,
            mapper.writeValueAsString(
                listOf(
                    BEGREP_TO_BE_CREATED.copy(versjonsnr = SemVer(0, 0, 0)),
                    BEGREP_TO_BE_CREATED
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])

        val error: Map<String, Any> = mapper.readValue(response["body"] as String)

        assertEquals(
            "Concept 0 - {}\n" +
                    "Invalid version 0.0.0. Version must be minimum 0.1.0\n\n", error["message"]
        )
    }
}

package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_CREATED
import no.fdk.concept_catalog.utils.BEGREP_WRONG_ORG
import no.fdk.concept_catalog.utils.Access
import no.fdk.concept_catalog.utils.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@Tag("contract")
class CreateConcepts : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest(
            "/begreper/import",
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            null, HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for read access`() {
        val response = authorizedRequest(
            "/begreper/import",
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Forbidden when at least one concept has non write access orgId`() {
        val response = authorizedRequest(
            "/begreper/import",
            mapper.writeValueAsString(listOf(BEGREP_WRONG_ORG, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Ok - Created - for write access`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_TO_BE_CREATED.ansvarligVirksomhet.id}",
            null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val response = authorizedRequest(
            "/begreper/import",
            mapper.writeValueAsString(listOf(BEGREP_TO_BE_CREATED, BEGREP_TO_BE_CREATED)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_TO_BE_CREATED.ansvarligVirksomhet.id}",
            null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> = mapper.readValue(before.body as String)
        val afterList: List<Begrep> = mapper.readValue(after.body as String)

        assertEquals(beforeList.size + 2, afterList.size)
    }

    @Test
    fun `Bad request - Invalid version - for write access`() {
        val response = authorizedRequest(
            "/begreper/import",
            mapper.writeValueAsString(
                listOf(
                    BEGREP_TO_BE_CREATED.copy(versjonsnr = SemVer(0, 0, 0)),
                    BEGREP_TO_BE_CREATED
                )
            ),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val error: Map<String, Any> = mapper.readValue(response.body as String)

        assertEquals(
            "Concept 0 - {}\n" +
                    "Invalid version 0.0.0. Version must be minimum 0.1.0\n\n", error["message"]
        )
    }

    @Test
    fun `Import RDF responds with unauthorized on missing access token`() {
        val response = authorizedRequest(
            "/begreper/123456789/import",
            "PREFIX",
            null,
            HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Import RDF responds with forbidden on invalid authority`() {
        val response = authorizedRequest(
            "/begreper/123456789/import",
            "PREFIX",
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Import RDF responds with forbidden on invalid catalog identifier`() {
        val response = authorizedRequest(
            "/begreper/invalid/import",
            "PREFIX",
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Import RDF responds with not implemented on valid authority`(@Value("classpath:concept.ttl") resource: Resource) {
        val concept = String(resource.inputStream.readAllBytes(), StandardCharsets.UTF_8)

        val response = authorizedRequest(
            "/begreper/123456789/import",
            concept,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.statusCode)
    }
}

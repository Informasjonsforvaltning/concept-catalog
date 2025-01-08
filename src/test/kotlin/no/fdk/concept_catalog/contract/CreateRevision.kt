package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class CreateRevision : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", port,
            mapper.writeValueAsString(BEGREP_REVISION), null, HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        operations.insert(BEGREP_4)

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", port, mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Bad request when attempting to create revision of unpublished concept`() {
        operations.insert(BEGREP_2)

        val response = authorizedRequest(
            "/begreper/${BEGREP_2.id}/revisjon", port, mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Bad request when attempting to create revision of concept with existing unpublished revision`() {
        operations.insert(BEGREP_HAS_REVISION)

        val response = authorizedRequest(
            "/begreper/${BEGREP_HAS_REVISION.id}/revisjon",
            port,
            mapper.writeValueAsString(BEGREP_UNPUBLISHED_REVISION),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Ok - Created with version - for write access`() {
        operations.insert(BEGREP_4)

        stubFor(post(urlMatching("/111222333/.*/updates")).willReturn(aResponse().withStatus(200)))

        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", port, mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.CREATED.value(), response["status"])

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> =
            mapper.readValue<List<Begrep>>(before["body"] as String).filter { it.id != "id5" }
        val afterList: List<Begrep> =
            mapper.readValue<List<Begrep>>(after["body"] as String).filter { it.id != "id5" }

        assertEquals(beforeList.size + 1, afterList.size)

        val revision: Begrep? = afterList.firstOrNull { it.id != "id4" }
        val concept4After: Begrep? = afterList.firstOrNull { it.id == "id4" }

        assertEquals(BEGREP_4.originaltBegrep, revision?.originaltBegrep)
        assertEquals(SemVer(1, 0, 1), revision?.versjonsnr)
        assertEquals(Status.UTKAST, revision?.status)
        assertEquals(false, revision?.erPublisert)
        assertEquals(BEGREP_REVISION.anbefaltTerm, revision?.anbefaltTerm)
        assertEquals(BEGREP_4.ansvarligVirksomhet, revision?.ansvarligVirksomhet)
        assertEquals(revision?.id, concept4After?.gjeldendeRevisjon)
    }

    @Test
    fun `Ok - Created without version - for write access`() {
        operations.insert(BEGREP_4)

        stubFor(post(urlMatching("/111222333/.*/updates")).willReturn(aResponse().withStatus(200)))

        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon",
            port,
            mapper.writeValueAsString(BEGREP_REVISION.copy(versjonsnr = null)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.CREATED.value(), response["status"])

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> =
            mapper.readValue<List<Begrep>>(before["body"] as String).filter { it -> it.id != "id5" }
        val afterList: List<Begrep> =
            mapper.readValue<List<Begrep>>(after["body"] as String).filter { it -> it.id != "id5" }

        assertEquals(beforeList.size + 1, afterList.size)

        val revision: Begrep? = afterList.firstOrNull { it.id != "id4" }
        val concept4After: Begrep? = afterList.firstOrNull { it.id == "id4" }

        assertEquals(BEGREP_4.originaltBegrep, revision?.originaltBegrep)
        assertEquals(SemVer(1, 0, 1), revision?.versjonsnr)
        assertEquals(Status.UTKAST, revision?.status)
        assertEquals(false, revision?.erPublisert)
        assertEquals(BEGREP_REVISION.anbefaltTerm, revision?.anbefaltTerm)
        assertEquals(BEGREP_4.ansvarligVirksomhet, revision?.ansvarligVirksomhet)
        assertEquals(revision?.id, concept4After?.gjeldendeRevisjon)
    }

    @Test
    fun `Bad request - Created with invalid version - for write access`() {
        operations.insert(BEGREP_4)

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon",
            port,
            mapper.writeValueAsString(BEGREP_REVISION.copy(versjonsnr = SemVer(1, 0, 0))),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])

        val error: Map<String, Any> = mapper.readValue(response["body"] as String)

        assertEquals("Invalid version 1.0.0. Version must be greater than 1.0.0", error["message"])
    }
}

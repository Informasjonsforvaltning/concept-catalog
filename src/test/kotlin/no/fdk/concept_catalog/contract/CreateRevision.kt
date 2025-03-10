package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.Access
import no.fdk.concept_catalog.utils.JwtToken
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
            "/begreper/${BEGREP_4.id}/revisjon",
            mapper.writeValueAsString(BEGREP_REVISION), null, HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for read access`() {
        mongoOperations.insert(BEGREP_4.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Bad request when attempting to create revision of unpublished concept`() {
        mongoOperations.insert(BEGREP_2.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_2.id}/revisjon", mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Bad request when attempting to create revision of concept with existing unpublished revision`() {
        mongoOperations.insert(BEGREP_HAS_REVISION.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_HAS_REVISION.id}/revisjon",

            mapper.writeValueAsString(BEGREP_UNPUBLISHED_REVISION),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Ok - Created with version - for write access`() {
        mongoOperations.insert(BEGREP_4.toDBO())

        stubFor(post(urlMatching("/111222333/.*/updates")).willReturn(aResponse().withStatus(200)))

        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon", mapper.writeValueAsString(BEGREP_REVISION),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> =
            mapper.readValue<List<Begrep>>(before.body as String).filter { it.id != "id5" }
        val afterList: List<Begrep> =
            mapper.readValue<List<Begrep>>(after.body as String).filter { it.id != "id5" }

        assertEquals(beforeList.size + 1, afterList.size)

        val revision: Begrep? = afterList.firstOrNull { it.id != "id4" }
        val concept4After: Begrep? = afterList.firstOrNull { it.id == "id4" }

        assertEquals(BEGREP_4.originaltBegrep, revision?.originaltBegrep)
        assertEquals(SemVer(1, 0, 1), revision?.versjonsnr)
        assertEquals(Status.UTKAST, revision?.status)
        assertEquals(false, revision?.erPublisert)
        assertEquals(BEGREP_REVISION.anbefaltTerm, revision?.anbefaltTerm)
        assertEquals(BEGREP_4.ansvarligVirksomhet, revision?.ansvarligVirksomhet)
    }

    @Test
    fun `Ok - Created without version - for write access`() {
        mongoOperations.insert(BEGREP_4.toDBO())

        stubFor(post(urlMatching("/111222333/.*/updates")).willReturn(aResponse().withStatus(200)))

        val before = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon",

            mapper.writeValueAsString(BEGREP_REVISION.copy(versjonsnr = null)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.CREATED, response.statusCode)

        val after = authorizedRequest(
            "/begreper?orgNummer=${BEGREP_4.ansvarligVirksomhet.id}",
            null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET
        )

        val beforeList: List<Begrep> =
            mapper.readValue<List<Begrep>>(before.body as String).filter { it -> it.id != "id5" }
        val afterList: List<Begrep> =
            mapper.readValue<List<Begrep>>(after.body as String).filter { it -> it.id != "id5" }

        assertEquals(beforeList.size + 1, afterList.size)

        val revision: Begrep? = afterList.firstOrNull { it.id != "id4" }
        val concept4After: Begrep? = afterList.firstOrNull { it.id == "id4" }

        assertEquals(BEGREP_4.originaltBegrep, revision?.originaltBegrep)
        assertEquals(SemVer(1, 0, 1), revision?.versjonsnr)
        assertEquals(Status.UTKAST, revision?.status)
        assertEquals(false, revision?.erPublisert)
        assertEquals(BEGREP_REVISION.anbefaltTerm, revision?.anbefaltTerm)
        assertEquals(BEGREP_4.ansvarligVirksomhet, revision?.ansvarligVirksomhet)
    }

    @Test
    fun `Bad request - Created with invalid version - for write access`() {
        mongoOperations.insert(BEGREP_4.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_4.id}/revisjon",

            mapper.writeValueAsString(BEGREP_REVISION.copy(versjonsnr = SemVer(1, 0, 0))),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)

        val error: Map<String, Any> = mapper.readValue(response.body as String)

        assertEquals("Invalid version 1.0.0. Version must be greater than 1.0.0", error["message"])
    }
}

package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_0_OLD
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
class GetConceptRevisions : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begreper/${BEGREP_0.id}/revisions", port, null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        operations.insert(BEGREP_WRONG_ORG)

        val response = authorizedRequest(
            "/begreper/${BEGREP_WRONG_ORG.id}/revisions",
            port,
            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Not found`() {
        val response = authorizedRequest(
            "/begreper/not-found/revisions",
            port,
            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun `Ok for read access`() {
        operations.insertAll(listOf(BEGREP_0, BEGREP_0_OLD))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}/revisions",
            port,
            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrep> = mapper.readValue(response["body"] as String)

        assertEquals(2, result.size)
        assertEquals(BEGREP_0, result[0])
        assertEquals(BEGREP_0_OLD, result[1])
    }

    @Test
    fun `Ok for write access`() {
        operations.insertAll(listOf(BEGREP_0, BEGREP_0_OLD))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}/revisions",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrep> = mapper.readValue(response["body"] as String)

        assertEquals(2, result.size)
        assertEquals(BEGREP_0, result[0])
        assertEquals(BEGREP_0_OLD, result[1])
    }
}

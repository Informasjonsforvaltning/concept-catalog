package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class GetConcepts : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begreper?orgNummer=123456789", port, null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val response = authorizedRequest(
            "/begreper?orgNummer=999888777",
            port, null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Ok for read access`() {
        operations.insertAll(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD))

        val response = authorizedRequest(
            "/begreper?orgNummer=123456789",
            port, null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrep> = mapper.readValue(response["body"] as String)

        assertEquals(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD), result)
    }

    @Test
    fun `Ok for write access`() {
        operations.insertAll(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD))

        val response = authorizedRequest(
            "/begreper?orgNummer=123456789",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: List<Begrep> = mapper.readValue(response["body"] as String)

        assertEquals(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD), result)
    }

    @Test
    fun `Ok for specific status`() {
        operations.insertAll(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD))

        val hearing = authorizedRequest(
            "/begreper?orgNummer=123456789&status=Høring",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val accepted = authorizedRequest(
            "/begreper?orgNummer=123456789&status=GODKJENT",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val published = authorizedRequest(
            "/begreper?orgNummer=123456789&status=publisert",
            port, null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), hearing["status"])
        assertEquals(HttpStatus.OK.value(), accepted["status"])
        assertEquals(HttpStatus.OK.value(), published["status"])

        val resultHearing: List<Begrep> = mapper.readValue(hearing["body"] as String)
        assertEquals(listOf(BEGREP_2), resultHearing)

        val resultAccepted: List<Begrep> = mapper.readValue(accepted["body"] as String)
        assertEquals(listOf(BEGREP_1), resultAccepted)

        val resultPublished: List<Begrep> = mapper.readValue(published["body"] as String)
        assertEquals(listOf(BEGREP_0, BEGREP_0_OLD), resultPublished)
    }
}

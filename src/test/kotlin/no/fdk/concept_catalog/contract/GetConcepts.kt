package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.utils.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class GetConcepts : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begreper?orgNummer=123456789", null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val response = authorizedRequest(
            "/begreper?orgNummer=999888777",
            null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Ok for read access`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_1.toDBO(), BEGREP_2.toDBO(), BEGREP_0_OLD.toDBO()))

        val response = authorizedRequest(
            "/begreper?orgNummer=123456789",
            null, JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrep> = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD), result)
    }

    @Test
    fun `Ok for write access`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_1.toDBO(), BEGREP_2.toDBO(), BEGREP_0_OLD.toDBO()))

        val response = authorizedRequest(
            "/begreper?orgNummer=123456789",
            null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrep> = mapper.readValue(response.body as String)

        assertEquals(listOf(BEGREP_0, BEGREP_1, BEGREP_2, BEGREP_0_OLD), result)
    }

    @Test
    fun `Ok for specific status`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_1.toDBO(), BEGREP_2.toDBO(), BEGREP_0_OLD.toDBO()))

        val hearing = authorizedRequest(
            "/begreper?orgNummer=123456789&status=Høring",
            null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val accepted = authorizedRequest(
            "/begreper?orgNummer=123456789&status=GODKJENT",
            null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val published = authorizedRequest(
            "/begreper?orgNummer=123456789&status=publisert",
            null, JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, hearing.statusCode)
        assertEquals(HttpStatus.OK, accepted.statusCode)
        assertEquals(HttpStatus.OK, published.statusCode)

        val resultHearing: List<Begrep> = mapper.readValue(hearing.body as String)
        assertEquals(listOf(BEGREP_2), resultHearing)

        val resultAccepted: List<Begrep> = mapper.readValue(accepted.body as String)
        assertEquals(listOf(BEGREP_1), resultAccepted)

        val resultPublished: List<Begrep> = mapper.readValue(published.body as String)
        assertEquals(listOf(BEGREP_0, BEGREP_0_OLD), resultPublished)
    }
}

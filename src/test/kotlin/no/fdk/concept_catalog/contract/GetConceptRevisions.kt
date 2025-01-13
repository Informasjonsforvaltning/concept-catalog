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
class GetConceptRevisions : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begreper/${BEGREP_0.id}/revisions", null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        mongoOperations.insert(BEGREP_WRONG_ORG.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_WRONG_ORG.id}/revisions",

            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Not found`() {
        val response = authorizedRequest(
            "/begreper/not-found/revisions",

            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `Ok for read access`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_0_OLD.toDBO()))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}/revisions",

            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrep> = mapper.readValue(response.body as String)

        assertEquals(2, result.size)
        assertEquals(BEGREP_0, result[0])
        assertEquals(BEGREP_0_OLD, result[1])
    }

    @Test
    fun `Ok for write access`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_0_OLD.toDBO()))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}/revisions",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: List<Begrep> = mapper.readValue(response.body as String)

        assertEquals(2, result.size)
        assertEquals(BEGREP_0, result[0])
        assertEquals(BEGREP_0_OLD, result[1])
    }
}

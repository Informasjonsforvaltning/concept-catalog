package no.fdk.conceptcatalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.conceptcatalog.ContractTestsBase
import no.fdk.conceptcatalog.model.Begrep
import no.fdk.conceptcatalog.model.toEntity
import no.fdk.conceptcatalog.utils.Access
import no.fdk.conceptcatalog.utils.BEGREP_0
import no.fdk.conceptcatalog.utils.BEGREP_WRONG_ORG
import no.fdk.conceptcatalog.utils.JwtToken
import no.fdk.conceptcatalog.utils.fromDBO
import no.fdk.conceptcatalog.utils.toDBO
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class GetConcept : ContractTestsBase() {
    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begreper/${BEGREP_0.id}", null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        conceptRepository.save(BEGREP_WRONG_ORG.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_WRONG_ORG.id}",
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Not found`() {
        val response =
            authorizedRequest(
                "/begreper/not-found",
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `Ok for read access`() {
        conceptRepository.save(BEGREP_0.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_0.id}",
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)
        assertEquals(BEGREP_0.fromDBO(), result)
    }

    @Test
    fun `Ok for write access`() {
        conceptRepository.save(BEGREP_0.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_0.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)
        assertEquals(BEGREP_0.fromDBO(), result)
    }
}

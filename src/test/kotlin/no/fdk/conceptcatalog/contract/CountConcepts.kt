package no.fdk.conceptcatalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.conceptcatalog.ContractTestsBase
import no.fdk.conceptcatalog.model.SearchOperation
import no.fdk.conceptcatalog.utils.Access
import no.fdk.conceptcatalog.utils.BEGREP_1
import no.fdk.conceptcatalog.utils.BEGREP_2
import no.fdk.conceptcatalog.utils.JwtToken
import no.fdk.conceptcatalog.utils.asCurrentConcept
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class CountConcepts : ContractTestsBase() {
    @Test
    fun `Unauthorized when access token is not included`() {
        val response =
            authorizedRequest(
                "/begreper/123456789/count",
                mapper.writeValueAsString(SearchOperation("test")),
                null,
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val response =
            authorizedRequest(
                "/begreper/999888777/count",
                mapper.writeValueAsString(SearchOperation("test")),
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Ok for read access`() {
        addToElasticsearchIndex(listOf(BEGREP_1.asCurrentConcept(), BEGREP_2.asCurrentConcept()))

        val response =
            authorizedRequest(
                "/begreper/123456789/count",
                mapper.writeValueAsString(SearchOperation("test")),
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Long = mapper.readValue(response.body as String)

        assertEquals(2, result)
    }
}

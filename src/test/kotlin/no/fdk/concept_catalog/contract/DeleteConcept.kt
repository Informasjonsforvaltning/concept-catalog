package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.utils.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals

@Tag("contract")
class DeleteConcept : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val response = authorizedRequest("/begreper/${BEGREP_0.id}", null, null, HttpMethod.DELETE)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        val response =
            authorizedRequest("/begreper/${BEGREP_0.id}", null, JwtToken(Access.ORG_READ).toString(), HttpMethod.DELETE)

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Bad request when published`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.DELETE
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Is deleted for write access`() {
        mongoOperations.insert(BEGREP_TO_BE_DELETED.toDBO())

        val before = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )
        assertEquals(HttpStatus.OK.value(), before["status"])

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.DELETE
        )
        assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])

        val after = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )
        assertEquals(HttpStatus.NOT_FOUND.value(), after["status"])
    }

}

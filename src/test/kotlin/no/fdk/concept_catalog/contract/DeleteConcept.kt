package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.CurrentConcept
import no.fdk.concept_catalog.model.Paginated
import no.fdk.concept_catalog.model.SearchOperation
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

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for read access`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        val response =
            authorizedRequest("/begreper/${BEGREP_0.id}", null, JwtToken(Access.ORG_READ).toString(), HttpMethod.DELETE)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
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

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
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
        assertEquals(HttpStatus.OK, before.statusCode)

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.DELETE
        )
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)

        val after = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )
        assertEquals(HttpStatus.NOT_FOUND, after.statusCode)
    }

    @Test
    fun `Previous version is added to search when current is deleted`() {
        mongoOperations.insertAll(listOf(BEGREP_0_OLD.toDBO(), BEGREP_0.copy(erPublisert = false).toDBO()))
        addToElasticsearchIndex(listOf(BEGREP_0.copy(erPublisert = false).asCurrentConcept()))

        val deleteResponse = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.DELETE
        )
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.statusCode)

        val searchResponse = authorizedRequest(
            "/begreper/search?orgNummer=123456789",

            mapper.writeValueAsString(SearchOperation("")),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.OK, searchResponse.statusCode)

        val expected = BEGREP_0_OLD.copy(
            sistPublisertId = BEGREP_0_OLD.id
        )

        val result: Paginated = mapper.readValue(searchResponse.body as String)
        assertEquals(listOf(expected), result.hits)
    }

}

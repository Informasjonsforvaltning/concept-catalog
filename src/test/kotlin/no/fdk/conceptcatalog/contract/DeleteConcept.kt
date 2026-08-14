package no.fdk.conceptcatalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.conceptcatalog.ContractTestsBase
import no.fdk.conceptcatalog.model.ChangeRequest
import no.fdk.conceptcatalog.model.CurrentConcept
import no.fdk.conceptcatalog.model.Paginated
import no.fdk.conceptcatalog.model.SearchOperation
import no.fdk.conceptcatalog.model.toEntity
import no.fdk.conceptcatalog.utils.Access
import no.fdk.conceptcatalog.utils.BEGREP_0
import no.fdk.conceptcatalog.utils.BEGREP_0_OLD
import no.fdk.conceptcatalog.utils.BEGREP_TO_BE_DELETED
import no.fdk.conceptcatalog.utils.BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED
import no.fdk.conceptcatalog.utils.CHANGE_REQUEST_TO_BE_DELETED
import no.fdk.conceptcatalog.utils.JwtToken
import no.fdk.conceptcatalog.utils.asCurrentConcept
import no.fdk.conceptcatalog.utils.toDBO
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
        conceptRepository.save(BEGREP_0.toDBO().toEntity())

        val response =
            authorizedRequest("/begreper/${BEGREP_0.id}", null, JwtToken(Access.ORG_READ).toString(), HttpMethod.DELETE)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Bad request when published`() {
        conceptRepository.save(BEGREP_0.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_0.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE,
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Is deleted for write access`() {
        conceptRepository.save(BEGREP_TO_BE_DELETED.toDBO().toEntity())

        val before =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )
        assertEquals(HttpStatus.OK, before.statusCode)

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE,
            )
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)

        val after =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )
        assertEquals(HttpStatus.NOT_FOUND, after.statusCode)
    }

    @Test
    fun `Is deleted with change request`() {
        conceptRepository.save(BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED.toDBO().toEntity())
        changeRequestRepository.save(CHANGE_REQUEST_TO_BE_DELETED)

        val before =
            authorizedRequest(
                "/begreper/${BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )
        assertEquals(HttpStatus.OK, before.statusCode)

        val beforeChangeRequest =
            authorizedRequest(
                "/111111111/endringsforslag?concept=${BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )

        assertEquals(HttpStatus.OK, beforeChangeRequest.statusCode)
        val beforeChangeRequestResult: List<ChangeRequest> = mapper.readValue(beforeChangeRequest.body as String)
        assertEquals(listOf(CHANGE_REQUEST_TO_BE_DELETED), beforeChangeRequestResult)

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE,
            )
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)

        val after =
            authorizedRequest(
                "/begreper/${BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )
        assertEquals(HttpStatus.NOT_FOUND, after.statusCode)

        val afterChangeRequest =
            authorizedRequest(
                "/111111111/endringsforslag?concept=${BEGREP_WITH_CHANGE_REQUEST_TO_BE_DELETED.id}",
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.GET,
            )
        assertEquals(HttpStatus.OK, afterChangeRequest.statusCode)

        val afterChangeRequestResult: List<ChangeRequest> = mapper.readValue(afterChangeRequest.body as String)
        assertEquals(emptyList(), afterChangeRequestResult)
    }

    @Test
    fun `Previous version is added to search when current is deleted`() {
        conceptRepository.saveAll(
            listOf(BEGREP_0_OLD.toDBO().toEntity(), BEGREP_0.copy(erPublisert = false, isArchived = false).toDBO().toEntity()),
        )
        addToElasticsearchIndex(listOf(BEGREP_0.copy(erPublisert = false, isArchived = false).asCurrentConcept()))

        val deleteResponse =
            authorizedRequest(
                "/begreper/${BEGREP_0.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.DELETE,
            )
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.statusCode)

        val searchResponse =
            authorizedRequest(
                "/begreper/search?orgNummer=123456789",
                mapper.writeValueAsString(SearchOperation("")),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )
        assertEquals(HttpStatus.OK, searchResponse.statusCode)

        val expected =
            BEGREP_0_OLD.copy(
                sistPublisertId = BEGREP_0_OLD.id,
            )

        val result: Paginated = mapper.readValue(searchResponse.body as String)
        assertEquals(listOf(expected), result.hits)
    }
}

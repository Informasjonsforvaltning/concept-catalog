package no.fdk.conceptcatalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.conceptcatalog.ContractTestsBase
import no.fdk.conceptcatalog.model.Begrep
import no.fdk.conceptcatalog.model.Paginated
import no.fdk.conceptcatalog.model.SearchFilter
import no.fdk.conceptcatalog.model.SearchFilters
import no.fdk.conceptcatalog.model.SearchOperation
import no.fdk.conceptcatalog.model.toEntity
import no.fdk.conceptcatalog.utils.Access
import no.fdk.conceptcatalog.utils.BEGREP_HAS_REVISION
import no.fdk.conceptcatalog.utils.BEGREP_TO_BE_DELETED
import no.fdk.conceptcatalog.utils.BEGREP_TO_BE_UPDATED
import no.fdk.conceptcatalog.utils.BEGREP_UNPUBLISHED_REVISION
import no.fdk.conceptcatalog.utils.BEGREP_WRONG_ORG
import no.fdk.conceptcatalog.utils.JwtToken
import no.fdk.conceptcatalog.utils.toDBO
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Tag("contract")
class PublishConcept : ContractTestsBase() {
    @Test
    fun `Unauthorized when access token is not included`() {
        val response =
            authorizedRequest("/begreper/${BEGREP_TO_BE_UPDATED.id}/publish", null, null, HttpMethod.POST)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        conceptRepository.save(BEGREP_WRONG_ORG.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_WRONG_ORG.id}/publish",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Not found`() {
        val response =
            authorizedRequest(
                "/begreper/not-found/publish",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `Forbidden for read access`() {
        conceptRepository.save(BEGREP_TO_BE_UPDATED.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",
                null,
                JwtToken(Access.ORG_READ).toString(),
                HttpMethod.POST,
            )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Bad request when publishing Concept that does not validate`() {
        conceptRepository.save(BEGREP_TO_BE_DELETED.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_DELETED.id}/publish",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Ok for write access`() {
        conceptRepository.save(BEGREP_TO_BE_UPDATED.toDBO().toEntity())

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertNotNull(result.publiseringsTidspunkt)

        val expected =
            BEGREP_TO_BE_UPDATED.copy(
                erPublisert = true,
                isArchived = true,
                sistPublisertId = null,
                publiseringsTidspunkt = result.publiseringsTidspunkt,
            )

        assertEquals(expected, result)

        // Elastic has been updated after publish

        val searchResponse =
            authorizedRequest(
                "/begreper/search?orgNummer=${BEGREP_TO_BE_UPDATED.ansvarligVirksomhet.id}",
                mapper.writeValueAsString(
                    SearchOperation(
                        "",
                        filters = SearchFilters(originalId = SearchFilter(listOf(BEGREP_TO_BE_UPDATED.originaltBegrep!!))),
                    ),
                ),
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )

        val searchResult: Paginated = mapper.readValue(searchResponse.body as String)

        val searchExpected = expected.copy(sistPublisertId = BEGREP_TO_BE_UPDATED.id)

        assertEquals(searchExpected, searchResult.hits.first())
    }

    @Test
    fun `Internal relations are changed to non-internal on publish`() {
        conceptRepository.saveAll(
            listOf(
                BEGREP_TO_BE_UPDATED.toDBO().toEntity(),
                BEGREP_HAS_REVISION.toDBO().toEntity(),
                BEGREP_UNPUBLISHED_REVISION.toDBO().toEntity(),
            ),
        )

        val response =
            authorizedRequest(
                "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.POST,
            )

        assertEquals(HttpStatus.OK, response.statusCode)

        val get0 =
            authorizedRequest(
                "/begreper/${BEGREP_HAS_REVISION.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )

        val result0: Begrep = mapper.readValue(get0.body as String)

        assertTrue(result0.internSeOgså?.none { it.contains(BEGREP_TO_BE_UPDATED.id!!) } ?: true)
        assertTrue(result0.seOgså!!.any { it.contains(BEGREP_TO_BE_UPDATED.id!!) })

        val get1 =
            authorizedRequest(
                "/begreper/${BEGREP_UNPUBLISHED_REVISION.id}",
                null,
                JwtToken(Access.ORG_WRITE).toString(),
                HttpMethod.GET,
            )

        val result1: Begrep = mapper.readValue(get1.body as String)

        assertTrue(result1.internErstattesAv?.none { it.contains(BEGREP_TO_BE_UPDATED.id!!) } ?: true)
        assertTrue(result1.erstattesAv!!.any { it.contains(BEGREP_TO_BE_UPDATED.id!!) })
        assertTrue(result1.begrepsRelasjon!!.any { it.relatertBegrep!!.contains(BEGREP_TO_BE_UPDATED.id!!) })
        assertTrue(result1.internBegrepsRelasjon!!.none { it.relatertBegrep!!.contains(BEGREP_TO_BE_UPDATED.id!!) })
    }
}

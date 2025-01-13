package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.Access
import no.fdk.concept_catalog.utils.JwtToken
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
        mongoOperations.insert(BEGREP_WRONG_ORG.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_WRONG_ORG.id}/publish",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Not found`() {
        val response = authorizedRequest(
            "/begreper/not-found/publish",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `Forbidden for read access`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",

            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Bad request when publishing Concept that does not validate`() {
        mongoOperations.insert(BEGREP_TO_BE_DELETED.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}/publish",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Ok for write access`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertNotNull(result.publiseringsTidspunkt)

        assertEquals(
            BEGREP_TO_BE_UPDATED.copy(
                erPublisert = true,
                erSistPublisert = true,
                sistPublisertId = "id-to-be-updated",
                publiseringsTidspunkt = result.publiseringsTidspunkt
            ), result
        )
    }

    @Test
    fun `Internal relations are changed to non-internal on publish`() {
        mongoOperations.insertAll(
            listOf(
                BEGREP_TO_BE_UPDATED.toDBO(),
                BEGREP_HAS_REVISION.toDBO(),
                BEGREP_UNPUBLISHED_REVISION.toDBO()
            )
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val get0 = authorizedRequest(
            "/begreper/${BEGREP_HAS_REVISION.id}",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val result0: Begrep = mapper.readValue(get0.body as String)

        assertTrue(result0.internSeOgså?.none { it.contains(BEGREP_TO_BE_UPDATED.id!!) } ?: true)
        assertTrue(result0.seOgså!!.any { it.contains(BEGREP_TO_BE_UPDATED.id!!) })

        val get1 = authorizedRequest(
            "/begreper/${BEGREP_UNPUBLISHED_REVISION.id}",

            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )

        val result1: Begrep = mapper.readValue(get1.body as String)

        assertTrue(result1.internErstattesAv?.none { it.contains(BEGREP_TO_BE_UPDATED.id!!) } ?: true)
        assertTrue(result1.erstattesAv!!.any { it.contains(BEGREP_TO_BE_UPDATED.id!!) })
        assertTrue(result1.begrepsRelasjon!!.any { it.relatertBegrep!!.contains(BEGREP_TO_BE_UPDATED.id!!) })
        assertTrue(result1.internBegrepsRelasjon!!.none { it.relatertBegrep!!.contains(BEGREP_TO_BE_UPDATED.id!!) })
    }
}

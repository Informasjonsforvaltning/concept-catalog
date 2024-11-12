package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_HAS_REVISION
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_DELETED
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_UPDATED
import no.fdk.concept_catalog.utils.BEGREP_UNPUBLISHED_REVISION
import no.fdk.concept_catalog.utils.BEGREP_WRONG_ORG
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val mapper = JacksonConfigurer().objectMapper()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class PublishConcept : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest("/begreper/${BEGREP_TO_BE_UPDATED.id}/publish", port, null, null, HttpMethod.POST)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for wrong orgnr`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_WRONG_ORG.id}/publish",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Not found`() {
        val rsp = authorizedRequest(
            "/begreper/not-found/publish",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.NOT_FOUND.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",
            port,
            null,
            JwtToken(Access.ORG_READ).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Bad request when publishing Concept that does not validate`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}/publish",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Ok for write access`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertNotNull(result.publiseringsTidspunkt)
        assertEquals(BEGREP_TO_BE_UPDATED.copy(
            erPublisert = true,
            erSistPublisert = true,
            sistPublisertId = "id-to-be-updated",
            publiseringsTidspunkt = result.publiseringsTidspunkt
        ), result)
    }

    @Test
    fun `Internal relations are changed to non-internal on publish`() {
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}/publish",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )
        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val get0 = authorizedRequest(
            "/begreper/${BEGREP_HAS_REVISION.id}",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )
        val result0: Begrep = mapper.readValue(get0["body"] as String)
        assertTrue(result0.internSeOgså?.none { it.contains(BEGREP_TO_BE_UPDATED.id!!) } ?: true)
        assertTrue(result0.seOgså!!.any { it.contains(BEGREP_TO_BE_UPDATED.id!!) })

        val get1 = authorizedRequest(
            "/begreper/${BEGREP_UNPUBLISHED_REVISION.id}",
            port,
            null,
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.GET
        )
        val result1: Begrep = mapper.readValue(get1["body"] as String)
        assertTrue(result1.internErstattesAv?.none { it.contains(BEGREP_TO_BE_UPDATED.id!!) } ?: true)
        assertTrue(result1.erstattesAv!!.any { it.contains(BEGREP_TO_BE_UPDATED.id!!) })
        assertTrue(result1.begrepsRelasjon!!.any { it.relatertBegrep!!.contains(BEGREP_TO_BE_UPDATED.id!!) })
        assertTrue(result1.internBegrepsRelasjon!!.none { it.relatertBegrep!!.contains(BEGREP_TO_BE_UPDATED.id!!) })
    }

}

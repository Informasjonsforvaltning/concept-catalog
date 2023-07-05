package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_DELETED
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_UPDATED
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
import kotlin.test.assertNull

private val mapper = JacksonConfigurer().objectMapper()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class UpdateConcept : ApiTestContext() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            null, HttpMethod.PATCH
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Add new values`() {
        val operations = listOf(
            JsonPatchOperation(op = OpEnum.ADD, "/anbefaltTerm/navn/nb", "Oppdatert"),
            JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", listOf("Ny merknad"))
        )

        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals("Oppdatert", result.anbefaltTerm?.navn?.get("nb"))
        assertEquals(listOf("Ny merknad"), result.merknad?.get("nb"))
    }

    @Test
    fun `Remove value`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REMOVE, "/tillattTerm/nn"))

        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertNull(result.tillattTerm?.get("nn"))
    }

    @Test
    fun `Replace existing value`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "Updated"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals("Updated", result.anbefaltTerm?.navn?.get("en"))
    }

    @Test
    fun `Move value`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.MOVE, path = "/frarådetTerm/en", from = "/tillattTerm/en"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals(BEGREP_TO_BE_UPDATED.tillattTerm?.get("en"), result.frarådetTerm?.get("en"))
        assertNull(result.tillattTerm?.get("en"))
    }

    @Test
    fun `Copy value`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", from = "/bruksområde/en"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals(BEGREP_TO_BE_UPDATED.bruksområde?.get("en"), result.bruksområde?.get("en"))
        assertEquals(BEGREP_TO_BE_UPDATED.bruksområde?.get("en"), result.eksempel?.get("en"))
    }

    @Test
    fun `Bad request when patch path is wrong`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", from = "/bruksområde/nn"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Bad request when patch value is invalid`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/eksempel/en", value = "invalid"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Bad request when concept is published`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/kildebeskrivelse", value = Kildebeskrivelse(ForholdTilKildeEnum.EGENDEFINERT, emptyList())))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Bad request when trying to publish as part of normal update`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/erPublisert", value = true))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Bad request when trying to add published date`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/publiseringsTidspunkt", value = "2020-01-02T12:00:00.000+01:00"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), rsp["status"])
    }

    @Test
    fun `Able to add new Kildebeskrivelse`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/definisjon/kildebeskrivelse", value = Kildebeskrivelse(ForholdTilKildeEnum.EGENDEFINERT, emptyList())))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])
    }

    @Test
    fun `Able to add new Bruker`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/tildeltBruker", value = Bruker(id="Test Testesen")))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals("Test Testesen", result.tildeltBruker?.id)
    }

    @Test
    fun `Replace tildeltBruker`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/tildeltBruker/id", "fdk bruker"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals("fdk bruker", result.tildeltBruker?.id)
    }

    @Test
    fun `Patch fails when history-service fails`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", listOf("Ny merknad")))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), rsp["status"])
    }

}

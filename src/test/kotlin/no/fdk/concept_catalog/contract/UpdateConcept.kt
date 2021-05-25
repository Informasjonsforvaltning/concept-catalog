package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.OpEnum
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_UPDATED
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val mapper = jacksonObjectMapper()

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
        assertEquals(listOf("Ny merknad"), result.merknad["nb"])
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
        assertNull(result.tillattTerm["nn"])
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
        assertEquals(BEGREP_TO_BE_UPDATED.tillattTerm["en"], result.frarådetTerm["en"])
        assertNull(result.tillattTerm["en"])
    }

    @Test
    fun `Copy value`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", value = listOf("asdads"), from = "/bruksområde/en"))
        val rsp = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: Begrep = mapper.readValue(rsp["body"] as String)
        assertEquals(BEGREP_TO_BE_UPDATED.bruksområde["en"], result.bruksområde["en"])
        assertEquals(BEGREP_TO_BE_UPDATED.bruksområde["en"], result.eksempel["en"])
    }

}

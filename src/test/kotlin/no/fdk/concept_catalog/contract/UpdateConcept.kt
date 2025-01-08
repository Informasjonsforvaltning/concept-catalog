package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Tag("contract")
class UpdateConcept : ContractTestsBase() {

    @Test
    fun `Unauthorized when access token is not included`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            null, HttpMethod.PATCH
        )

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun `Add new values`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(
            JsonPatchOperation(op = OpEnum.ADD, "/anbefaltTerm/navn/nb", "Oppdatert"),
            JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad")
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)

        assertEquals("Oppdatert", result.anbefaltTerm?.navn?.get("nb"))
        assertEquals("Ny merknad", result.merknad?.get("nb"))
    }

    @Test
    fun `Remove value`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.REMOVE, "/tillattTerm/nn"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)

        assertNull(result.tillattTerm?.get("nn"))
    }

    @Test
    fun `Replace existing value`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "Updated"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)

        assertEquals("Updated", result.anbefaltTerm?.navn?.get("en"))
    }

    @Test
    fun `Move value`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations =
            listOf(JsonPatchOperation(op = OpEnum.MOVE, path = "/frarådetTerm/en", from = "/tillattTerm/en"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)

        assertEquals(BEGREP_TO_BE_UPDATED.tillattTerm?.get("en"), result.frarådetTerm?.get("en"))
        assertNull(result.tillattTerm?.get("en"))
    }

    @Test
    fun `Copy value`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations =
            listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", from = "/anbefaltTerm/navn/en"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)

        assertEquals(BEGREP_TO_BE_UPDATED.anbefaltTerm?.navn?.get("en"), result.anbefaltTerm?.navn?.get("en"))
        assertEquals(BEGREP_TO_BE_UPDATED.anbefaltTerm?.navn?.get("en"), result.eksempel?.get("en"))
    }

    @Test
    fun `Bad request when patch path is wrong`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        val operations = listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", from = "/bruksområde/nn"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Bad request when patch value is invalid`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        val operations =
            listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/eksempel/en", value = listOf("invalid")))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Bad request when concept is published`() {
        operations.insert(BEGREP_0)

        val operations = listOf(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/kildebeskrivelse",
                value = Kildebeskrivelse(ForholdTilKildeEnum.EGENDEFINERT, emptyList())
            )
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Bad request when trying to publish as part of normal update`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/erPublisert", value = true))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Bad request when trying to add published date`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        val operations = listOf(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/publiseringsTidspunkt",
                value = "2020-01-02T12:00:00.000+01:00"
            )
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }

    @Test
    fun `Able to add new Kildebeskrivelse`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/definisjon/kildebeskrivelse",
                value = Kildebeskrivelse(ForholdTilKildeEnum.EGENDEFINERT, emptyList())
            )
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])
    }

    @Test
    fun `Able to add new Bruker`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "user-id"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)

        assertEquals("user-id", result.assignedUser)
    }

    @Test
    fun `Replace tildeltBruker`() {
        operations.insert(BEGREP_TO_BE_UPDATED)

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/assignedUser", "new user"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK.value(), response["status"])

        val result: Begrep = mapper.readValue(response["body"] as String)
        assertEquals("new user", result.assignedUser)
    }

    @Test
    fun `Patch fails when history-service fails`() {
        operations.insert(BEGREP_TO_BE_DELETED)

        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response["status"])
    }

    @Test
    fun `Patch of published concept creates new revision`() {
        operations.insert(BEGREP_0)

        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.CREATED.value(), response["status"])

        val responseHeaders: HttpHeaders = response["header"] as HttpHeaders
        val location = responseHeaders.location

        assertNotNull(location)

        val getRsp = authorizedRequest(
            location.toString(),
            port, null,
            JwtToken(Access.ORG_READ).toString(), HttpMethod.GET
        )

        assertEquals(HttpStatus.OK.value(), getRsp["status"])
    }

    @Test
    fun `Bad request when patching old version`() {
        operations.insertAll(listOf(BEGREP_0, BEGREP_0_OLD))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0_OLD.id}",
            port, mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
    }
}

package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.utils.*
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
            mapper.writeValueAsString(operations),
            null, HttpMethod.PATCH
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden for read access`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Add new values`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(
            JsonPatchOperation(op = OpEnum.ADD, "/anbefaltTerm/navn/nb", "Oppdatert"),
            JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad")
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertEquals("Oppdatert", result.anbefaltTerm?.navn?.get("nb"))
        assertEquals("Ny merknad", result.merknad?.get("nb"))
    }

    @Test
    fun `Remove value`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.REMOVE, "/tillattTerm/nn"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertNull(result.tillattTerm?.get("nn"))
    }

    @Test
    fun `Replace existing value`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "Updated"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertEquals("Updated", result.anbefaltTerm?.navn?.get("en"))
    }

    @Test
    fun `Move value`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations =
            listOf(JsonPatchOperation(op = OpEnum.MOVE, path = "/frarådetTerm/en", from = "/tillattTerm/en"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertEquals(BEGREP_TO_BE_UPDATED.tillattTerm?.get("en"), result.frarådetTerm?.get("en"))
        assertNull(result.tillattTerm?.get("en"))
    }

    @Test
    fun `Copy value`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations =
            listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", from = "/anbefaltTerm/navn/en"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertEquals(BEGREP_TO_BE_UPDATED.anbefaltTerm?.navn?.get("en"), result.anbefaltTerm?.navn?.get("en"))
        assertEquals(BEGREP_TO_BE_UPDATED.anbefaltTerm?.navn?.get("en"), result.eksempel?.get("en"))
    }

    @Test
    fun `Bad request when patch path is wrong`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val operations = listOf(JsonPatchOperation(op = OpEnum.COPY, path = "/eksempel/en", from = "/bruksområde/nn"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Bad request when patch value is invalid`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val operations =
            listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/eksempel/en", value = listOf("invalid")))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Bad request when concept is published`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        val operations = listOf(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/kildebeskrivelse",
                value = Kildebeskrivelse(ForholdTilKildeEnum.EGENDEFINERT, emptyList())
            )
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Bad request when trying to publish as part of normal update`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, path = "/erPublisert", value = true))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Bad request when trying to add published date`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        val operations = listOf(
            JsonPatchOperation(
                op = OpEnum.ADD,
                path = "/publiseringsTidspunkt",
                value = "2020-01-02T12:00:00.000+01:00"
            )
        )

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Able to add new Kildebeskrivelse`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

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
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `Able to add new Bruker`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, path = "/assignedUser", value = "user-id"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)

        assertEquals("user-id", result.assignedUser)
    }

    @Test
    fun `Replace tildeltBruker`() {
        mongoOperations.insert(BEGREP_TO_BE_UPDATED.toDBO())

        stubFor(post(urlMatching("/111111111/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/assignedUser", "new user"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_UPDATED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.OK, response.statusCode)

        val result: Begrep = mapper.readValue(response.body as String)
        assertEquals("new user", result.assignedUser)
    }

    @Test
    fun `Patch fails when history-service fails`() {
        mongoOperations.insert(BEGREP_TO_BE_DELETED.toDBO())

        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_TO_BE_DELETED.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `Patch of published concept creates new revision`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val responseHeaders: HttpHeaders = response.headers
        val location = responseHeaders.location

        assertNotNull(location)

        val getRsp = authorizedRequest(
            location.toString(),
            null,
            JwtToken(Access.ORG_READ).toString(), HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, getRsp.statusCode)
    }

    @Test
    fun `Bad request when patching old version`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_0_OLD.toDBO()))

        val operations = listOf(JsonPatchOperation(op = OpEnum.ADD, "/merknad/nb", "Ny merknad"))

        val response = authorizedRequest(
            "/begreper/${BEGREP_0_OLD.id}",
            mapper.writeValueAsString(operations),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
}

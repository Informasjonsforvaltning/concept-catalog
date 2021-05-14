package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.OpEnum
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

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
        val rsp = authorizedRequest("/begreper/id0", port, mapper.writeValueAsString(operations), null, HttpMethod.PATCH)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `Forbidden for read access`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))
        val rsp =
            authorizedRequest("/begreper/id0", port, mapper.writeValueAsString(operations), JwtToken(Access.ORG_READ).toString(), HttpMethod.PATCH)

        assertEquals(HttpStatus.FORBIDDEN.value(), rsp["status"])
    }

    @Test
    fun `Ok for write access`() {
        val operations = listOf(JsonPatchOperation(op = OpEnum.REPLACE, "/anbefaltTerm/navn/en", "req"))
        val rsp =
            authorizedRequest("/begreper/id0", port, mapper.writeValueAsString(operations), JwtToken(Access.ORG_WRITE).toString(), HttpMethod.PATCH)

        assertEquals(HttpStatus.OK.value(), rsp["status"])
    }

}
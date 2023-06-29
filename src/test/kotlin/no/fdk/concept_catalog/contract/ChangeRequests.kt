package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import kotlin.test.assertEquals
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Begrepssamling
import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.CHANGE_REQUEST_0
import no.fdk.concept_catalog.utils.authorizedRequest
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

private val mapper = JacksonConfigurer().objectMapper()


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class ChangeRequests : ApiTestContext() {

    @Nested
    internal inner class GetCatalogRequests {
        private val path = "/111111111/endringsforslag"

         @Test
         fun unauthorizedWhenMissingToken() {
             val rsp = authorizedRequest(path, port, null,null, HttpMethod.GET )

             assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
         }


        @Test
        fun ableToGetChangeRequestsForAllOrgRoles() {
            val rspRead = authorizedRequest(path, port, null,JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            val rspWrite = authorizedRequest(path, port, null,JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.OK.value(), rspRead["status"])
            assertEquals(HttpStatus.OK.value(), rspWrite["status"])
            val resultRead: List<ChangeRequest> = mapper.readValue(rspRead["body"] as String)
            val resultWrite: List<ChangeRequest> = mapper.readValue(rspWrite["body"] as String)

            val expected = listOf(CHANGE_REQUEST_0)
            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }

        @Test
        fun ableToGetChangeRequestForAllOrgRoles() {
            val rspRead = authorizedRequest("$path/${CHANGE_REQUEST_0.id}", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET )
            val rspWrite = authorizedRequest("$path/${CHANGE_REQUEST_0.id}", port, null,JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET )

            assertEquals(HttpStatus.OK.value(), rspRead["status"])
            assertEquals(HttpStatus.OK.value(), rspWrite["status"])
            val resultRead: ChangeRequest = mapper.readValue(rspRead["body"] as String)
            val resultWrite: ChangeRequest = mapper.readValue(rspWrite["body"] as String)

            val expected = CHANGE_REQUEST_0
            assertEquals(expected, resultRead)
            assertEquals(expected, resultWrite)
        }

        @Test
        fun ableToDeleteChangeRequest() {
            val rspWrite = authorizedRequest("$path/${CHANGE_REQUEST_0.id}", port, null,JwtToken(Access.ORG_WRITE).toString(), HttpMethod.DELETE )
            assertEquals(HttpStatus.NO_CONTENT.value(), rspWrite["status"])
        }
    }
}
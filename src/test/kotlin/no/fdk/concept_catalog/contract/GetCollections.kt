package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Begrepssamling
import no.fdk.concept_catalog.utils.*
import no.fdk.concept_catalog.utils.jwk.Access
import no.fdk.concept_catalog.utils.jwk.JwtToken
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

private val mapper = JacksonConfigurer().objectMapper()

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class GetCollections : ApiTestContext() {

    @BeforeAll
    fun resetDatabase() {
        resetDB()
    }

    @Test
    fun `Unauthorized when access token is not included`() {
        val rsp = authorizedRequest("/begrepssamlinger", port, null, null, HttpMethod.GET)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), rsp["status"])
    }

    @Test
    fun `All collections for root access`() {
        val rsp = authorizedRequest("/begrepssamlinger", port, null, JwtToken(Access.ROOT).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrepssamling> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(
            Begrepssamling("111111111", 2),
            Begrepssamling("111222333", 1),
            Begrepssamling("123456789", 3),
            Begrepssamling("999888777", 1)
        ), result.sortedBy { it.id })
    }

    @Test
    fun `Only permitted collections for write access`() {
        val rsp = authorizedRequest("/begrepssamlinger", port, null, JwtToken(Access.ORG_WRITE).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrepssamling> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(
            Begrepssamling("111111111", 2),
            Begrepssamling("111222333", 1),
            Begrepssamling("123456789", 3)
        ), result.sortedBy { it.id })
    }

    @Test
    fun `Only permitted collections for read access`() {
        val rsp = authorizedRequest("/begrepssamlinger", port, null, JwtToken(Access.ORG_READ).toString(), HttpMethod.GET)

        assertEquals(HttpStatus.OK.value(), rsp["status"])

        val result: List<Begrepssamling> = mapper.readValue(rsp["body"] as String)
        assertEquals(listOf(
            Begrepssamling("111111111", 2),
            Begrepssamling("111222333", 1),
            Begrepssamling("123456789", 3)
        ), result.sortedBy { it.id })
    }

}

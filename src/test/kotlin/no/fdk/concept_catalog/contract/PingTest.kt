package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.apiGet
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class PingTest: ApiTestContext() {

    @Test
    fun ping() {
        val response = apiGet(port, "/ping", MediaType.TEXT_PLAIN)

        assertTrue { HttpStatus.OK.value() == response["status"] }
    }
    @Test
    fun ready() {
        val response = apiGet(port, "/ready", MediaType.TEXT_PLAIN)

        assertTrue { HttpStatus.OK.value() == response["status"] }
    }

}

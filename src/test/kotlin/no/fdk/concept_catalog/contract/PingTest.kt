package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.ContractTestsBase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import kotlin.test.assertEquals

@Tag("contract")
class PingTest : ContractTestsBase() {

    @Test
    fun ping() {
        val entity = request("/ping", MediaType.TEXT_PLAIN, HttpMethod.GET)

        assertEquals(HttpStatus.OK, entity.statusCode)
    }

    @Test
    fun ready() {
        val entity = request("/ready", MediaType.TEXT_PLAIN, HttpMethod.GET)

        assertEquals(HttpStatus.OK, entity.statusCode)
    }
}

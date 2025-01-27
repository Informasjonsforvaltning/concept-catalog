package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.model.ImportResultStatus
import no.fdk.concept_catalog.utils.Access
import no.fdk.concept_catalog.utils.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import kotlin.test.assertEquals

@Tag("contract")
class ImportControllerTests : ContractTestsBase() {

    val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `Unauthorized on missing access token`() {
        val response = authorizedRequest(
            path = "/import/123456789",
            httpMethod = HttpMethod.POST
        )

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Forbidden on invalid authority`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val response = authorizedRequest(
            path = "/import/123456789",
            body = turtle,
            token = JwtToken(Access.ORG_READ).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Forbidden on invalid catalog identifier`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val response = authorizedRequest(
            path = "/import/987654321",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `Unsupported media type on invalid rdf format`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val response = authorizedRequest(
            path = "/import/123456789",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("application/json")
        )

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.statusCode)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
            "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
    )
    fun `Created with location on valid rdf`(mediaType: String) {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val response = authorizedRequest(
            path = "/import/123456789",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf(mediaType)
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val statusResponse = authorizedRequest(
            path = response.headers.location.toString(),
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, statusResponse.statusCode)

        val importResult = objectMapper.readValue(statusResponse.body, ImportResult::class.java)

        assertEquals(ImportResultStatus.COMPLETED, importResult!!.status)
    }

    @Test
    fun `Created with location on on invalid turtle`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept .
        """.trimIndent()

        val response = authorizedRequest(
            path = "/import/123456789",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val statusResponse = authorizedRequest(
            path = response.headers.location.toString(),
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, statusResponse.statusCode)

        val importResult = objectMapper.readValue(statusResponse.body, ImportResult::class.java)

        assertEquals(ImportResultStatus.FAILED, importResult!!.status)
    }
}

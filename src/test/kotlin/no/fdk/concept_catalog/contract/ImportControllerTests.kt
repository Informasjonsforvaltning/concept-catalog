package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
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

    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

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

    @Test
    fun `Bad request on invalid rdf`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        .
        """.trimIndent()

        val response = authorizedRequest(
            path = "/import/123456789",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @ParameterizedTest
    @ValueSource(
        strings = ["text/turtle", "text/n3", "application/rdf+json", "application/ld+json", "application/rdf+xml",
            "application/n-triples", "application/n-quads", "application/trig", "application/trix"]
    )
    fun `Created with location on minimum viable skos-ap-no`(mediaType: String) {
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

        assertEquals(1, importResult.extractionRecords.size)
        val extractionRecord = importResult.extractionRecords.first()

        val conceptResponse = authorizedRequest(
            path = "/begreper/${extractionRecord.internalId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, conceptResponse.statusCode)

        val concept = objectMapper.readValue(conceptResponse.body, Begrep::class.java)

        assertEquals("123456789", concept.ansvarligVirksomhet.id)
        assertEquals("anbefaltTerm", concept.anbefaltTerm!!.navn["nb"])
    }

    @Test
    fun `Created with location on invalid skos-ap-no`() {
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

    @Test
    fun `Created with location on maximum viable skos-ap-no`() {
        val turtle = """
            @prefix owl:   <http://www.w3.org/2002/07/owl#> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix skosno: <https://data.norge.no/vocabulary/skosno#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .
            @prefix dct:   <http://purl.org/dc/terms/> .
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix dcat:  <http://www.w3.org/ns/dcat#> .
            @prefix euvoc:  <http://publications.europa.eu/ontology/euvoc#> .
            @prefix relationship-with-source-type: <https://data.norge.no/vocabulary/relationship-with-source-type#> .
            @prefix audience-type: <https://data.norge.no/vocabulary/audience-type#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    euvoc:status          <http://publications.europa.eu/resource/authority/concept-status/CURRENT> ;
                    owl:versionInfo       "1.0.0" ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    skos:altLabel         "tillattTerm"@nb ;
                    skos:hiddenLabel      "fraraadetTerm"@nb ;
                    skos:scopeNote        "merknad"@nb ;
                    skos:example          "eksempel"@nb ;
                    dct:subject           "fagområde"@nb ;
                    skosno:valueRange     "omfang"@nb ;
                    euvoc:startDate       "2020-12-31"^^xsd:date ;
                    euvoc:endDate         "2030-12-31"^^xsd:date ;
                    dcat:contactPoint     
                          [ 
                            rdf:type                vcard:Organization ;
                            vcard:hasEmail          <mailto:organization@example.com> ;
                            vcard:hasTelephone      <tel:+123-456-789> ;
                          ] ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon"@nb ;
                            skosno:relationshipWithSource   relationship-with-source-type:self-composed ;
                            dct:source                      "kap14", <https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14> ;
                          ] ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon for spesialister"@nb ;
                            dct:audience                    audience-type:specialist ;
                            skosno:relationshipWithSource   relationship-with-source-type:direct-from-source ;
                          ] ;
                    euvoc:xlDefinition                    
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon for allmennheten"@nb ;
                            dct:audience                    audience-type:public ;
                            skosno:relationshipWithSource   relationship-with-source-type:derived-from-source ;
                          ] .
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

        assertEquals(ImportResultStatus.COMPLETED, importResult!!.status)
    }
}

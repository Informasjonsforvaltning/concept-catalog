package no.fdk.concept_catalog.contract

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.model.ImportResultStatus
import no.fdk.concept_catalog.model.Paginated
import no.fdk.concept_catalog.model.SearchOperation
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Term
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.utils.Access
import no.fdk.concept_catalog.utils.JwtToken
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@Tag("contract")
class ImportControllerTests : ContractTestsBase() {

    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    val importId = UUID.randomUUID().toString()
    val catalogId = "123456789"

    @Test
    fun `Unauthorized on missing access token`() {
        val response = authorizedRequest(
            path = "/import/123456789/${importId}",
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
            path = "/import/123456789/${importId}",
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

        val invalidCatalogId = "987654321"
        val importResultOnGoing = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = invalidCatalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResultOnGoing)

        val response = authorizedRequest(
            path = "/import/${invalidCatalogId}/${importId}",
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
            path = "/import/${catalogId}/${importId}",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.APPLICATION_ATOM_XML
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
            path = "/import/${catalogId}/${importId}",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Created with location on minimum viable skos-ap-no`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val importResultOnGoing = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResultOnGoing)

        val response = authorizedRequest(
            path = "/import/${catalogId}/${importId}",
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

        val importResultPending = objectMapper.readValue(statusResponse.body, ImportResult::class.java)

        assertEquals(ImportResultStatus.PENDING_CONFIRMATION, importResultPending!!.status)

        val statusResponseConfirmSave = authorizedRequest(
            path = "/import/${catalogId}/${importId}/confirm",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.PUT
        )

        assertEquals(HttpStatus.CREATED, statusResponseConfirmSave.statusCode)

        val statusResponseImportResult = authorizedRequest(
            path = "/import/${catalogId}/results/${importId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        val importResultCompleted = objectMapper.readValue(statusResponseImportResult.body,
            ImportResult::class.java)

        assertEquals(1, importResultCompleted.extractionRecords.size)
        val extractionRecord = importResultCompleted.extractionRecords.first()

        val conceptResponse = authorizedRequest(
            path = "/begreper/${extractionRecord.internalId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, conceptResponse.statusCode)

        val concept = objectMapper.readValue(conceptResponse.body, Begrep::class.java)

        assertEquals(catalogId, concept.ansvarligVirksomhet.id)
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

        val importResultOnGoing = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResultOnGoing)

        val response = authorizedRequest(
            path = "/import/123456789/${importId}",
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
    fun `Updated with location on minimum viable skos-ap-no`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val importResultOnGoing = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResultOnGoing)

        val response = authorizedRequest(
            path = "/import/${catalogId}/${importId}",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        authorizedRequest(
            path = "/import/${catalogId}/${importId}/confirm",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val updateTurtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "oppdatertAnbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        val importIdUpdate = UUID.randomUUID().toString()
        importResultRepository.save(importResultOnGoing.copy(id = importIdUpdate))

        val updateResponse = authorizedRequest(
            path = "/import/${catalogId}/${importIdUpdate}",
            body = updateTurtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.CREATED, updateResponse.statusCode)

        authorizedRequest(
            path = "/import/${catalogId}/${importIdUpdate}/confirm",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.PUT,
            contentType = MediaType.valueOf("text/turtle")
        )


        val countResponse = authorizedRequest(
            path = "/import/${catalogId}/results",
            body = updateTurtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET,
            contentType = MediaType.valueOf(APPLICATION_JSON_VALUE)
        )

        assertEquals(HttpStatus.OK, countResponse.statusCode)

        val importResults = objectMapper.readValue(countResponse.body, object : TypeReference<List<ImportResult>>() {})
        assertEquals(2, importResults.size)



        val statusResponse = authorizedRequest(
            path = "/import/${catalogId}/results/${importIdUpdate}",
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
        assertEquals("oppdatertAnbefaltTerm", concept.anbefaltTerm!!.navn["nb"])
    }

    @Test
    fun `Created with location on maximum viable skos-ap-no`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val turtle = """
            @prefix owl:   <http://www.w3.org/2002/07/owl#> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix skosno: <https://data.norge.no/vocabulary/skosno#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .
            @prefix dct:   <http://purl.org/dc/terms/> .
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
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
                    rdfs:seeAlso          <https://example.com/seeAlsoConcept> ;
                    dct:isReplacedBy      <https://example.com/isReplacedByConcept> ;
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
                          ] ;
                    skosno:isFromConceptIn 
                          [ 
                            rdf:type                        skosno:AssociativeConceptRelation ;
                            skosno:hasToConcept             <https://example.com/topConcept> ; 
                            skosno:relationRole             "muliggjør"@nb ;
                          ] ;
                    skosno:hasPartitiveConceptRelation    
                          [ 
                            rdf:type                        skosno:PartitiveConceptRelation ;
                            dct:description                 "inndelingskriterium"@nb ;
                            skosno:hasPartitiveConcept      <https://example.com/partitiveConcept>
                          ] ;
                    skosno:hasPartitiveConceptRelation    
                          [ 
                            rdf:type                        skosno:PartitiveConceptRelation ;
                            dct:description                 "inndelingskriterium"@nb ;
                            skosno:hasComprehensiveConcept  <https://example.com/comprehensiveConcept>
                          ] ;
                    skosno:hasGenericConceptRelation      
                          [ 
                            rdf:type                        skosno:GenericConceptRelation ;
                            dct:description                 "inndelingskriterium"@nb ;
                            skosno:hasGenericConcept        <https://example.com/genericConcept>
                          ] ;
                    skosno:hasGenericConceptRelation     
                          [ 
                            rdf:type                        skosno:GenericConceptRelation ;
                            dct:description                 "inndelingskriterium"@nb ;
                            skosno:hasSpecificConcept       <https://example.com/specificConcept>
                          ] .
        """.trimIndent()

        val importResultOnGoing = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResultOnGoing)

        val response = authorizedRequest(
            path = "/import/${catalogId}/${importId}",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)

        authorizedRequest(
            path = "/import/${catalogId}/${importId}/confirm",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.PUT
        )

        val statusResponse = authorizedRequest(
            path = response.headers.location.toString(),
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, statusResponse.statusCode)

        val importResult = objectMapper.readValue(statusResponse.body, ImportResult::class.java)

        assertEquals(ImportResultStatus.COMPLETED, importResult!!.status)

        val begreperResponse = authorizedRequest(
            path = "/begreper/search?orgNummer=123456789",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            body = objectMapper.writeValueAsString(
                SearchOperation(query = "")
            )
        )

        // get number of concepts in the response
        val searchHits: Paginated = objectMapper.readValue(
            begreperResponse.body,
            Paginated::class.java
        )

        assertEquals(HttpStatus.OK, begreperResponse.statusCode)
        assertNotEquals(0, searchHits?.hits?.size)

    }

    @Test
    fun `should find ImportResult and should remove it`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val turtle = """
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix dct:   <http://purl.org/dc/terms/> .
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix euvoc:  <http://publications.europa.eu/ontology/euvoc#> .
            
            <https://example.com/concept>
                    rdf:type            skos:Concept ;
                    rdfs:seeAlso        <http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322> ;
                    dct:isReplacedBy    <http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322> ;
                    euvoc:status        <http://publications.europa.eu/resource/authority/concept-status/CURRENT> ;
                    skos:altLabel       "tillattTerm"@nn, "tillattTerm2"@nn ;
                    skos:hiddenLabel    "fraraadetTerm"@nb, "fraraadetTerm2"@nb, "Lorem ipsum"@nb .
        """.trimIndent()

        val importResult = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.COMPLETED,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResult)

        val responseTester: (Int) -> List<ImportResult> = { resultsSize ->
            val importResultsResponse = authorizedRequest(
                path = "/import/${catalogId}/results",
                token = JwtToken(Access.ORG_WRITE).toString(),
                httpMethod = HttpMethod.GET,
            )

            assertEquals(HttpStatus.OK, importResultsResponse.statusCode)

            val importResults: List<ImportResult> = objectMapper.readValue(
                importResultsResponse.body,
                object : TypeReference<List<ImportResult>>() {}
            )

            assertEquals(resultsSize, importResults.size)

            importResults
        }

        authorizedRequest(
            path = "/import/${catalogId}",
            body = turtle,
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.POST,
            contentType = MediaType.valueOf("text/turtle")
        )

        var importResults: List<ImportResult> = responseTester(1)

        val importResultId = importResults.map { it.id }.first()

        val deleteImportResult = authorizedRequest(
            path = "/import/${catalogId}/results/$importResultId",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.DELETE
        )

        assertEquals(HttpStatus.NO_CONTENT, deleteImportResult.statusCode)

        responseTester(0)

    }

    @Test
    fun `Forbidden for read access`() {

        val BEGREP_TO_IMPORT = Begrep(
            id = "http://example.com/begrep/123456789",
            status = Status.UTKAST,
            statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
            anbefaltTerm = Term(navn = emptyMap()),
            ansvarligVirksomhet = Virksomhet(
                id = catalogId
            )
        )

        val response = authorizedRequest(
            "/import/${catalogId}/${importId}",
            mapper.writeValueAsString(listOf(BEGREP_TO_IMPORT)),
            JwtToken(Access.ORG_READ).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `should fail to create import for non admin org`() {
        val response = authorizedRequest(
            path = "/import/${catalogId}/createImportId",
            token = JwtToken(Access.ORG_READ).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    @Test
    fun `should create import for admin org`() {
        val response = createImportResult()

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val importId = response?.headers?.get("location")?.first()?.split("/")?.last() ?: ""

        assertNotNull(importId)

        val responseImportResult = authorizedRequest(
            path = "/import/${catalogId}/results/${importId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertEquals(HttpStatus.OK, responseImportResult.statusCode)

        val importResultOngoing = mapper.readValue(responseImportResult.body, ImportResult::class.java)

        assertEquals(ImportResultStatus.IN_PROGRESS, importResultOngoing?.status)
    }

    fun createImportResult(id: String? = null, access: Access? = null) = authorizedRequest(
        path = "/import/${id?: catalogId}/createImportId",
        token = JwtToken(access?: Access.ORG_WRITE).toString(),
        httpMethod = HttpMethod.GET
    )

    @Test
    fun `should cancel import for admin org only`() {
        val response = createImportResult()

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val importId = response?.headers?.get("location")?.first()?.split("/")?.last() ?: ""

        assertNotNull(importId)

        val responseCancelForbidden = authorizedRequest(
            path = "/import/${catalogId}/${importId}/cancel",
            token = JwtToken(Access.ORG_READ).toString(),
            httpMethod = HttpMethod.PUT
        )

        assertEquals(HttpStatus.FORBIDDEN, responseCancelForbidden.statusCode)

        val responseCancel = authorizedRequest(
            path = "/import/${catalogId}/${importId}/cancel",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.PUT
        )

        assertEquals(HttpStatus.CREATED, responseCancel.statusCode)

        val responseImportResult = authorizedRequest(
            path = "/import/${catalogId}/results/${importId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        val importResultCancelled = mapper.readValue(responseImportResult.body, ImportResult::class.java)

        assertEquals(ImportResultStatus.CANCELLED, importResultCancelled?.status)

    }

    @Test
    fun `should confirm import for admin org only`() {
        val response = createImportResult()

        assertEquals(HttpStatus.CREATED, response.statusCode)

        val importId = response?.headers?.get("location")?.first()?.split("/")?.last() ?: ""

        var responseImportResult = authorizedRequest(
            path = "/import/${catalogId}/results/${importId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        assertNotNull(importId)

        val importResultOngoing = mapper.readValue(responseImportResult.body, ImportResult::class.java)
        importResultRepository.save(importResultOngoing.copy(status = ImportResultStatus.PENDING_CONFIRMATION))

        val responseForbidden = authorizedRequest(
            path = "/import/${catalogId}/${importId}/confirm",
            token = JwtToken(Access.ORG_READ).toString(),
            httpMethod = HttpMethod.PUT
        )

        assertEquals(HttpStatus.FORBIDDEN, responseForbidden.statusCode)

        val responseConfirm = authorizedRequest(
            path = "/import/${catalogId}/${importId}/confirm",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.PUT
        )

        assertEquals(HttpStatus.CREATED, responseConfirm.statusCode)

        responseImportResult = authorizedRequest(
            path = "/import/${catalogId}/results/${importId}",
            token = JwtToken(Access.ORG_WRITE).toString(),
            httpMethod = HttpMethod.GET
        )

        val importResultCompleted = mapper.readValue(responseImportResult.body, ImportResult::class.java)
        assertEquals(ImportResultStatus.COMPLETED, importResultCompleted?.status)

    }

    @Test
    fun `Success for org admin access`() {
        stubFor(post(urlMatching("/123456789/.*/updates")).willReturn(aResponse().withStatus(200)))

        val importResult = ImportResult(
            id = importId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            extractionRecords = emptyList()
        )
        importResultRepository.save(importResult)

        val BEGREP_TO_IMPORT = Begrep(
            id = "http://example.com/begrep/123456789",
            status = Status.UTKAST,
            statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
            anbefaltTerm = Term(navn = emptyMap()),
            ansvarligVirksomhet = Virksomhet(
                id = catalogId
            )
        )

        val response = authorizedRequest(
            "/import/${catalogId}/${importId}",
            mapper.writeValueAsString(listOf(BEGREP_TO_IMPORT)),
            JwtToken(Access.ORG_WRITE).toString(),
            HttpMethod.POST
        )

        assertEquals(HttpStatus.CREATED, response.statusCode)
    }

    @Test
    fun `User is unauthorized to save concept for organization he does not have access for`() {
        val resultId = UUID.randomUUID().toString()

        val BEGREP_TO_IMPORT = Begrep(
            id = "http://example.com/begrep/123456789",
            status = Status.UTKAST,
            statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
            anbefaltTerm = Term(navn = emptyMap()),
            ansvarligVirksomhet = Virksomhet(
                id = "987654321" // different organization ID from catalogId
            )
        )

        val response = authorizedRequest(
            "/import/${catalogId}/${resultId}",
            mapper.writeValueAsString(listOf(BEGREP_TO_IMPORT)),
            JwtToken(Access.ORG_WRITE).toString(), HttpMethod.POST
        )

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }
}

package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.User
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.repository.ImportResultRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.apache.jena.riot.Lang
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import kotlin.test.assertNotNull
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.model.ImportResultStatus
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.fail


@Tag("unit")
class ImportServiceTest {
    private val historyService = mock<HistoryService>()
    private val conceptRepository = mock<ConceptRepository>()
    private val conceptService = mock<ConceptService>()
    private val importResultRepository = mock<ImportResultRepository>()
    private val objectMapper = JacksonConfigurer().objectMapper()

    private val importService = ImportService(
        historyService = historyService,
        conceptRepository = conceptRepository,
        conceptService = conceptService,
        importResultRepository = importResultRepository,
        objectMapper = objectMapper
    )
    private val jwt: Jwt = mock()

    @BeforeEach
    fun setupMockResponse() {
        whenever(jwt.tokenValue).thenReturn("mocked-token")
        whenever(conceptRepository.save(any())).thenAnswer { invocation ->
            invocation.arguments[0]
        }
        whenever(importResultRepository.save(any())).thenAnswer { invocation ->
            invocation.arguments[0] as ImportResult
        }
    }


    @Test
    fun `should fail when the same RDF is uploaded multiple times`() {

        val turtle = """
        @prefix schema: <http://schema.org/> .
        @prefix dct:   <http://purl.org/dc/terms/> .
        @prefix skosxl: <http://www.w3.org/2008/05/skos-xl#> .
        @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
        @prefix skosno: <http://difi.no/skosno#> .
        @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
        @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
        @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .
        @prefix dcat:  <http://www.w3.org/ns/dcat#> .
        @prefix xkos:  <http://rdf-vocabulary.ddialliance.org/xkos#> .

        <http://test/begrep/9c33fd2b-2964-11e6-b2bc-96405985e0fa>
         a                              skos:Concept ;
          skos:prefLabel "nytt begrep 9"@nb ;
          skosno:betydningsbeskrivelse  [ a                       skosno:Definisjon ;
            rdfs:label              "kostnader til oppmåling av regnskapsbasert fastsetting av boligeiendom for innrapportering av arealopplysninger ved formuesverdsettelse av boligen"@nb ;
            skosno:forholdTilKilde  <basertPåKilde> ;
            dct:source              [ rdfs:label  "RF-1189 rettledningen punkt 2.7"@nb ]
          ] ;
          skosno:datastrukturterm        "kostnadTilOppmåling"@nb ;
          dct:identifier                 "9c33fd2b-2964-11e6-b2bc-96405985e0fa" ;
          dct:modified                   "2017-09-04"^^xsd:date ;
          dct:publisher                  <https://data.brreg.no/enhetsregisteret/api/enheter/974761076> ;
          dct:subject                    "Formues- og inntektsskatt"@nb ;
          skosxl:prefLabel               [ a                   skosxl:Label ;
            skosxl:literalForm  "kostnad til oppmåling"@nb
          ] ;
          dcat:contactPoint              [ a                       vcard:Organization ;
            vcard:hasEmail          <mailto:test@skatteetaten.no> ;
            vcard:organizationUnit  "Informasjonsforvaltning - innhenting"
          ] .
        """.trimIndent()
        val catalogId = "123456789"
        val lang = Lang.TURTLE
        val user = User(id = "1924782563", name = "TEST USER", email = null)

        val importResultSuccess = importService.importRdf(
            catalogId = catalogId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt
        )

        assertNotNull(importResultSuccess)
        assertEquals(ImportResultStatus.COMPLETED, importResultSuccess.status)


        val importResultFailed = importService.importRdf(
            catalogId = catalogId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt
        )

        assertNotNull(importResultFailed)
        assertEquals(ImportResultStatus.COMPLETED, importResultFailed.status)
    }

    @Test
    fun `should throw exception if catalog id is wrong or ImportResultId is not found`() {
        val catalogId = "123456789"
        var importResultId = UUID.randomUUID().toString()


        val importResult = ImportResult(
            id = importResultId,
            created = LocalDateTime.now(),
            catalogId = catalogId,
            status = ImportResultStatus.COMPLETED
        )

        whenever(importResultRepository.findById(importResultId))
            .thenReturn(Optional.of(importResult))

        assertThrows(ResponseStatusException::class.java) {
            importService.deleteImportResult(catalogId, UUID.randomUUID().toString())
        }

        assertThrows(ResponseStatusException::class.java) {
            importService.deleteImportResult(catalogId.plus('0') , importResultId)
        }

        assertDoesNotThrow {
            try {
                importService.deleteImportResult(catalogId, importResultId)
            } catch (e: ResponseStatusException) {
                fail("it should not throw exception when catalogId and ImportResultId are correct")
            }
        }
    }
}
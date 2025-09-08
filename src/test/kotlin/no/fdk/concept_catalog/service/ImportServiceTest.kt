package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
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
import no.fdk.concept_catalog.elastic.CurrentConceptRepository
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.model.ImportResultStatus
import no.fdk.concept_catalog.model.IssueType
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.OpEnum
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Term
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.utils.BEGREP_TO_BE_CREATED
import org.junit.jupiter.api.Assertions.assertFalse
import org.springframework.http.HttpStatus
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.fail
import org.mockito.kotlin.verify
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.spy
import org.springframework.data.mongodb.core.MongoOperations
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    val importId = UUID.randomUUID().toString()
    val catalogId = "123456789"
    val externalId = "9c33fd2b-2964-11e6-b2bc-96405985e0fa"
    val conceptUri = "http://test/begrep/$externalId"
    val turtle = """
        @prefix schema: <http://schema.org/> .
        @prefix dct:   <http://purl.org/dc/terms/> .
        @prefix skosxl: <http://www.w3.org/2008/05/skos-xl#> .
        @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
        @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
        @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
        @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .
        @prefix dcat:  <http://www.w3.org/ns/dcat#> .
        @prefix xkos:  <http://rdf-vocabulary.ddialliance.org/xkos#> .

        <$conceptUri>
         a                              skos:Concept ;
          skos:prefLabel "nytt begrep 9"@nb ;
          dct:identifier                 "$externalId" ;
          dct:modified                   "2017-09-04"^^xsd:date ;
          dct:publisher                  <https://data.brreg.no/enhetsregisteret/api/enheter/$catalogId> ;
          dct:subject                    "Formues- og inntektsskatt"@nb ;
          dcat:contactPoint              [ a                       vcard:Organization ;
            vcard:hasEmail          <mailto:test@skatteetaten.no> ;
            vcard:organizationUnit  "Informasjonsforvaltning - innhenting"
          ] .
        """.trimIndent()

    val lang = Lang.TURTLE
    val user = User(id = "1924782563", name = "TEST USER", email = null)


    val begrepToImport = Begrep(
        id = conceptUri,
        status = Status.UTKAST,
        statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
        anbefaltTerm = Term(navn = mapOf("nb" to "Testnavn")),
        ansvarligVirksomhet = Virksomhet(
            uri = conceptUri,
            id = catalogId
        ),
        interneFelt = null,
        internErstattesAv = null,
    )

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

    @Test
    fun `should succeed when importing concepts that has not been imported before`() {
        val catalogId = "123456789"
        val conceptUri = "http://example.com/begrep/123456789"
        val virksomhetsUri = "http://example.com/begrep/123456789"
        val user = User(id = catalogId, name = "TEST USER", email = null)
        val begrepToImport = Begrep(
            id = conceptUri,
            status = Status.UTKAST,
            statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
            anbefaltTerm = Term(navn = mapOf("nb" to "Testnavn")),
            ansvarligVirksomhet = Virksomhet(
                uri = virksomhetsUri,
                id = catalogId
            ),
            interneFelt = null,
            internErstattesAv = null,
        )

        val importResultSuccess = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt)
        assertNotNull(importResultSuccess)
        assertEquals(ImportResultStatus.COMPLETED, importResultSuccess.status)
        assertFalse(importResultSuccess.extractionRecords.isEmpty())

    }

    @Test
    fun `should fail when importing same concept twice`() {
        val catalogId = "123456789"
        val conceptUri = "http://example.com/begrep/123456789"
        val user = User(id = catalogId, name = "TEST USER", email = null)
        val begrepToImport = Begrep(
            id = conceptUri,
            status = Status.UTKAST,
            statusURI = "http://publications.europa.eu/resource/authority/concept-status/DRAFT",
            anbefaltTerm = Term(navn = mapOf("nb" to "Testnavn")),
            ansvarligVirksomhet = Virksomhet(
                uri = conceptUri,
                id = catalogId
            ),
            interneFelt = null,
            internErstattesAv = null,
        )

        val conceptService = ConceptService(
            conceptRepository = conceptRepository,
            conceptSearchService = mock<ConceptSearchService>(),
            currentConceptRepository = mock<CurrentConceptRepository>(),
            mongoOperations = mock<MongoOperations>(),
            applicationProperties = ApplicationProperties("", "", ""),
            conceptPublisher = mock<ConceptPublisher>(),
            historyService = historyService,
            mapper = objectMapper
        )

        val importService = ImportService(
            historyService = historyService,
            conceptRepository = conceptRepository,
            conceptService = conceptService,
            importResultRepository = importResultRepository,
            objectMapper = objectMapper
        )

        val begrepCaptor = argumentCaptor<Iterable<BegrepDBO>>()

        val importResultSuccess = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt)

        whenever(conceptRepository.saveAll(any<Iterable<BegrepDBO>>())).thenAnswer {
            it.arguments[0] // return the same list
        }
        verify(conceptRepository).saveAll(begrepCaptor.capture())

        assertNotNull(importResultSuccess)
        assertEquals(ImportResultStatus.COMPLETED, importResultSuccess.status)
        assertFalse(importResultSuccess.extractionRecords.isEmpty())

        val begrepDBO: BegrepDBO? = begrepCaptor.firstValue.firstOrNull()
        assertNotNull(begrepDBO)

        val internalId = importResultSuccess.extractionRecords.first().internalId
        val originaltBegrep = begrepDBO.originaltBegrep

        whenever(
            importResultRepository.findFirstByStatusAndExtractionRecordsExternalId(
                ImportResultStatus.COMPLETED,
                conceptUri
            )
        ).thenReturn(importResultSuccess)
        whenever(conceptRepository.findById(internalId))
            .thenReturn(Optional.of(begrepDBO))
        whenever(conceptRepository.getByOriginaltBegrep(originaltBegrep))
            .thenReturn(listOf(begrepDBO))

        val importResultFailure = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt)

        assertNotNull(importResultFailure)
        assertEquals(ImportResultStatus.FAILED, importResultFailure.status)
    }

    @Test
    fun `should fail when the import result has no patch operations`() {
        val importResultOngoing = createImportResultInProgress()

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultOngoing))

        val importResultFailure = importService.importConcepts(
            concepts = listOf(createNewConcept(BEGREP_TO_BE_CREATED.ansvarligVirksomhet, user)
                .toDTO()
                .copy(id = conceptUri)
            ),
            catalogId = "123456789", user, jwt, importId)

        assertEquals(ImportResultStatus.FAILED, importResultFailure.status)
    }

    @Test
    fun `should fail when no concepts were included in the import`() {
        val importResultOngoing = createImportResultInProgress()

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultOngoing))

        val importResultFailure = importService.importConcepts(
            concepts = emptyList(),
            catalogId = "123456789", user, jwt, importId)

        assertEquals(ImportResultStatus.FAILED, importResultFailure.status)
    }

    @Test
    fun `Should create issue with error when version number is invalid`() {
        val begrepDBO = createNewConcept(BEGREP_TO_BE_CREATED.ansvarligVirksomhet, user)
            .copy(versjonsnr = SemVer(0, 0, 0)
        )

        val issues = importService.extractIssues(begrepDBO, emptyList<JsonPatchOperation>())

        assertTrue (issues.any { it.message.contains("Invalid version") })

    }

    @Test
    fun `should return error importing concepts with invalid organization`() {
        val begrepDBO = createNewConcept(BEGREP_TO_BE_CREATED.ansvarligVirksomhet, user)
            .copy(
                versjonsnr = SemVer(0, 1, 0),
                ansvarligVirksomhet = Virksomhet("", "", "", ""),
            )

        val issues = importService.extractIssues(begrepDBO, listOf<JsonPatchOperation>(
            JsonPatchOperation(OpEnum.TEST, path = "Test"))
        )

        assertTrue (issues.any { it.type == IssueType.ERROR })
        assertEquals (1, issues.filter { it.type == IssueType.ERROR }.size)

    }


    @Test
    fun `should raise exception when history service fails`() {
        val importResultOngoing = createImportResultInProgress()

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultOngoing))
        doThrow(RuntimeException("History service failed"))
            .whenever(historyService)
            .updateHistory(any(), any(), any(), any())

        val importResultPending = importService.importRdf(
            catalogId = catalogId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt,
            importId = importId
        )

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultPending))

        assertThrows<ResponseStatusException> {
            importService.confirmImportAndSave(catalogId, importId, user, jwt)
        }.also { assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, it.statusCode) }

    }

    @Test
    //@Disabled
    fun `should fail to rollback when exception is thrown during import`() {
        val importService = createImportServiceSpy()
        val importResultOngoing = createImportResultInProgress()

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultOngoing))

        val importResultPending = importService.importRdf(
            catalogId = catalogId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt,
            importId = importId
        )

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultPending))

        doThrow(RuntimeException("History service failed"))
            .whenever(historyService)
            .updateHistory(any(), any(), any(), any())

        doThrow(RuntimeException("History service failed"))
            .whenever(historyService)
            .removeHistoryUpdate(any(), any())

        assertThrows<Exception> {
            importService.confirmImportAndSave(catalogId, importId, user, jwt)
        }

    }

    @Test
    fun `should fail to rollback when exception is thrown updating DB`() {
        val importService = createImportServiceSpy()
        val importResultOngoing = createImportResultInProgress()

        doThrow(RuntimeException("Updating DB failed"))
            .whenever(conceptRepository)
            .saveAll(any<Iterable<BegrepDBO>>())
        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultOngoing ))

        val importResultPending = importService.importRdf(
            catalogId = catalogId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt,
            importId = importId
        )

        assertNotNull(importResultPending)
        assertEquals(ImportResultStatus.PENDING_CONFIRMATION, importResultPending.status)
        assertThrows<ResponseStatusException> {
            importService.confirmImportAndSave(catalogId, importId, user, jwt)
        }.also {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, it.statusCode)
        }

        verify(importService).rollbackHistoryUpdates(any(), any())
    }

    @Test
    fun `should fail to rollback when exception is thrown updating elastic`() {
        val importService = createImportServiceSpy()
        val importResultOngoing = createImportResultInProgress()

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultOngoing ))
        doThrow(RuntimeException("Updating DB failed"))
            .whenever(conceptRepository)
            .saveAll(any<Iterable<BegrepDBO>>())

        val importResultPending = importService.importRdf(
            catalogId = catalogId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt,
            importId = importId
        )

        whenever(importResultRepository.findById(importId))
            .thenReturn(Optional.of(importResultPending ))

        assertThrows<ResponseStatusException> {
            importService.confirmImportAndSave(
                catalogId = catalogId,
                importId = importId,
                user = user,
                jwt = jwt
            )
        }.also {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, it.statusCode)
        }

        verify(importService).rollbackHistoryUpdates(any(), any())

    }

    @Test
    fun `should throw response exceptions and roll back if Elastic fails`() {
        val importService = createImportServiceSpy()

        doThrow(RuntimeException("Fail Elastic"))
            .whenever(conceptService)
            .updateCurrentConceptForOriginalId(any<String>())

        var importResultUnknown: ImportResult? = null

        val exception = assertThrows<ResponseStatusException> {
            importResultUnknown = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt)
        }

        verify(conceptService).updateCurrentConceptForOriginalId(any<String>())
        verify(importService).rollBackUpdates(any(), any(),
            any(), any())

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.statusCode)
        assertNull(importResultUnknown)

    }

    @Test
    fun `Should throw response exceptions and roll back if DB fails to update`() {
        val importService = createImportServiceSpy()

        doThrow(RuntimeException("Fail DB"))
            .whenever(conceptRepository)
            .saveAll(any<Iterable<BegrepDBO>>())

        var importResultUnknown: ImportResult? = null

        val exception = assertThrows<ResponseStatusException> {
            importResultUnknown = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt)
        }

        verify(importService).saveAllConceptsDB(any())
        verify(importService).rollBackUpdates(any(), any(),
            any(), any())

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.statusCode)
        assertNull(importResultUnknown)

    }

    private fun createImportServiceSpy() = spy(ImportService(
        historyService = historyService,
        conceptRepository = conceptRepository,
        conceptService = conceptService,
        importResultRepository = importResultRepository,
        objectMapper = objectMapper
    ))

    private fun createImportResult(status: ImportResultStatus) = ImportResult(
        id = importId,
        created = LocalDateTime.now(),
        catalogId = catalogId,
        status = status
    )

    private fun createImportResultInProgress() = createImportResult(ImportResultStatus.IN_PROGRESS)


}
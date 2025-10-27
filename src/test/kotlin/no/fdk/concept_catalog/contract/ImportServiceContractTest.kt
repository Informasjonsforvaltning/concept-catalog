package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.model.ImportResultStatus
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.model.Term
import no.fdk.concept_catalog.model.User
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.service.ConceptService
import no.fdk.concept_catalog.service.HistoryService
import no.fdk.concept_catalog.service.ImportService
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@Tag("contract")
class ImportServiceContractTest : ContractTestsBase() {
    private val logger: Logger = LoggerFactory.getLogger(ImportServiceContractTest::class.java)

    private val historyService = mock<HistoryService>()
    private val conceptService = mock<ConceptService>()

    lateinit private var importService: ImportService

    private val jwt: Jwt = mock()

    val catalogId = "123456789"
    val importId = UUID.randomUUID().toString()
    val externalId = UUID.randomUUID().toString()
    val conceptUri = "http://example.com/begrep/123456789"
    val virksomhetsUri = "http://example.com/begrep/123456789"
    val user = User(id = catalogId, name = "TEST USER", email = null)
    val lang = Lang.TURTLE
    //val user = User(id = "1924782563", name = "TEST USER", email = null)

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

    @BeforeEach
    override fun setUp() {
        super.setUp()

        importService = ImportService(
            historyService = historyService,
            conceptRepository = conceptRepository,
            conceptService = conceptService,
            importResultRepository = importResultRepository,
            objectMapper = mapper
        )
    }

    @Test
    fun `should create import result when starting new import`() {
        importService.createImportResult(catalogId)
        assertEquals(1, importResultRepository.findAll().size)
    }

    @Test
    fun `should succeed when importing concepts that has not been imported before`() {
        val importResultOngoing = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultOngoing)

        val importResultWaiting = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        assertNotNull(importResultWaiting)
        assertEquals(ImportResultStatus.PENDING_CONFIRMATION, importResultWaiting.status)
        assertFalse(importResultWaiting.extractionRecords.isEmpty())

        importService.confirmImportAndSave(catalogId, importId, user, jwt)

        val importResultCompleted = importResultRepository.findById(importId)?.let { it.get() }
        assertNotNull(importResultCompleted)
        assertEquals(ImportResultStatus.COMPLETED, importResultCompleted.status)


    }

    @Test
    fun `should save concept after confirmation`() {
        val importResultOngoing = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultOngoing)
        importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        importService.confirmImportAndSave(catalogId, importId, user, jwt)

        assertEquals(1, conceptRepository.findAll().size)

    }

    @Test
    fun `should fail to import a concept that was imported before`() {
        val importResultOngoing = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultOngoing)
        val importResultPending = importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        assertFalse { importResultPending.extractionRecords.isEmpty() }
        assertFalse { importResultPending.conceptExtractions.isEmpty() }
        assertEquals(
            ImportResultStatus.PENDING_CONFIRMATION,
            importResultRepository.findById(importId)?.let { it.get() }?.status
        )

        importService.confirmImportAndSave(catalogId, importId, user, jwt)
        val importResultCompleted = importResultRepository.findById(importId)?.let { it.get() }

        assertEquals(1, conceptRepository.findAll().size)
        assertNotNull(importResultCompleted)
        assertEquals(ImportResultStatus.COMPLETED, importResultCompleted.status)

        val newImportResultOngoing = importService.createImportResult(catalogId)
        val importResultFailed = importService.importConcepts(listOf(begrepToImport), catalogId,
            user, jwt, newImportResultOngoing.id)

        assertEquals(ImportResultStatus.FAILED, importResultFailed.status)
        assertEquals(1, conceptRepository.findAll().size)
        assertEquals(2, importResultRepository.findAll().size)

    }

    @Test
    fun `should fail to process and throw exception if there is no import result with in progress`() {
        assertThrows<ResponseStatusException> {
            importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        }.also {
            assertEquals(HttpStatus.NOT_FOUND, it.statusCode)
        }

        val importResultCancelled = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.CANCELLED,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultCancelled)

        assertThrows<ResponseStatusException> {
            importService.importConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        }.also {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, it.statusCode)
        }

    }


    @Test
    fun `should not cancel import if does not exist`() {
        assertThrows<ResponseStatusException> {
            importService.cancelImport(importId)
        }.also {
            assertEquals(HttpStatus.NOT_FOUND, it.statusCode)
        }
    }

    @Test
    fun `should cancel import`() {

        val importResultInProgress = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultInProgress)

        importService.cancelImport(importId)

        assertEquals(ImportResultStatus.CANCELLED,
            importResultRepository.findById(importId)?.let { it.get() }?.status)

    }

    @Test
    fun `should fail when the same RDF is uploaded multiple times`() {
        val importResultOngoing = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultOngoing)
        importService.importRdf(
            catalogId = catalogId,
            importId = importId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt
        )
        val importResultWaiting = importResultRepository.findById(importId).get()

        assertEquals(importResultWaiting.extractedConcepts, importResultWaiting.totalConcepts)

        importService.confirmImportAndSave(catalogId, importId, user, jwt)

        val importIdNew = UUID.randomUUID().toString()
        val importResultOngoingNew = ImportResult(
            id = importIdNew,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultOngoingNew)
        importService.importRdf(
            catalogId = catalogId,
            importId = importIdNew,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt
        )

        val importResultFailed = importResultRepository.findById(importIdNew).let { it.get() }

        assertEquals(ImportResultStatus.FAILED, importResultFailed.status)
    }

    @Test
    fun `should raise exception when history service fails`() {
        val importResultOngoing = ImportResult(
            id = importId,
            catalogId = catalogId,
            status = ImportResultStatus.IN_PROGRESS,
            created = LocalDateTime.now()
        )

        importResultRepository.save(importResultOngoing)
        importService.importRdf(
            catalogId = catalogId,
            importId = importId,
            concepts = turtle,
            lang = lang,
            user = user,
            jwt = jwt
        )

        doThrow(RuntimeException("History service failed"))
            .whenever(historyService)
            .updateHistory(any(), any(), any(), any())

        doThrow(RuntimeException("History service failed"))
            .whenever(historyService)
            .removeHistoryUpdate(any(), any())

        assertThrows <Exception>{
            importService.confirmImportAndSave(catalogId, importId, user, jwt)
        }
    }
}
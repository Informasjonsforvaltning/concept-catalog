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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
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

    private val historyService = mock<HistoryService>()
    private val conceptService = mock<ConceptService>()

    lateinit private var importService: ImportService

    private val jwt: Jwt = mock()

    val catalogId = "123456789"
    val importId = UUID.randomUUID().toString()
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

        val importResultWaiting = importService.importAndProcessConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        assertNotNull(importResultWaiting)
        assertEquals(ImportResultStatus.PENDING_CONFIRMATION, importResultWaiting.status)
        assertFalse(importResultWaiting.extractionRecords.isEmpty())

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
        importService.importAndProcessConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
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
        importService.importAndProcessConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)

        assertEquals(
            ImportResultStatus.PENDING_CONFIRMATION,
            importResultRepository.findById(importId)?.let { it.get() }?.status
        )

        importService.confirmImportAndSave(catalogId, importId, user, jwt)

        assertEquals(1, conceptRepository.findAll().size)
        assertEquals(ImportResultStatus.COMPLETED, importResultRepository.findById(importId)?.let { it.get() }?.status)

        val newImportResultOngoing = importService.createImportResult(catalogId)
        val importResultFailed = importService.importAndProcessConcepts(listOf(begrepToImport), catalogId,
            user, jwt, newImportResultOngoing.id)

        assertEquals(ImportResultStatus.FAILED, importResultFailed.status)
        assertEquals(1, conceptRepository.findAll().size)
        assertEquals(2, importResultRepository.findAll().size)

    }

    @Test
    fun `should fail to process and throw exception if there is no import result with in progress`() {
        assertThrows<ResponseStatusException> {
            importService.importAndProcessConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
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
            importService.importAndProcessConcepts(listOf(begrepToImport), catalogId, user, jwt, importId)
        }.also {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, it.statusCode)
        }

    }

}
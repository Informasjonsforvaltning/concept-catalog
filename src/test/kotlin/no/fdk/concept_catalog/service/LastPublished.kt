package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.utils.BEGREP_3
import no.fdk.concept_catalog.utils.BEGREP_4
import no.fdk.concept_catalog.utils.BEGREP_5
import no.fdk.concept_catalog.utils.toDBO
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoOperations
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("unit")
class LastPublished {
    private val conceptRepository: ConceptRepository = mock()
    private val conceptSearch: ConceptSearchService = mock()
    private val mongoOperations: MongoOperations = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val conceptPublisher: ConceptPublisher = mock()

    private val conceptService = ConceptService(
        conceptRepository, conceptSearch, mongoOperations, applicationProperties, conceptPublisher, JacksonConfigurer().objectMapper())

    @Test
    fun `Able to get a list with the highest version of concepts for a publisher`() {
        whenever(conceptRepository.getBegrepByAnsvarligVirksomhetId("111222333"))
            .thenReturn(listOf(BEGREP_3, BEGREP_4, BEGREP_3.copy(id = "id3-2", versjonsnr = SemVer(2, 10, 0), revisjonAv = "id3-1"),
                BEGREP_3.copy(id = "id3-1", versjonsnr = SemVer(1, 9, 1), revisjonAv = "id3"), BEGREP_5,
                BEGREP_4.copy(id = "id4-1", versjonsnr = SemVer(1, 0, 1), revisjonAv = "id4"),
                BEGREP_4.copy(id = "id4-2", versjonsnr = SemVer(3, 0, 0), revisjonAv = "id4-1"),
                BEGREP_5.copy(id = "id5-1", versjonsnr = SemVer(9, 9, 1), revisjonAv = "id5"),
                BEGREP_5.copy(id = "id5-2", versjonsnr = SemVer(12, 10, 0), revisjonAv = "id5-1")).map { it.toDBO() })

        val result = conceptService.getLastPublishedForOrganization("111222333").map { it.id }

        assertTrue { result.size == 3 }
        assertTrue { result.contains("id3-2") }
        assertTrue { result.contains("id4-2") }
        assertTrue { result.contains("id5-2") }
    }

    @Test
    fun `Sets revision of last published for relevant concepts`() {
        val newPublished = BEGREP_3.copy(id = "id3-1", versjonsnr = SemVer(1, 9, 1), revisjonAv = "id3")
        val invalid = BEGREP_3.copy(id = "id3-2", versjonsnr = SemVer(2, 10, 0), revisjonAv = "id3", status = Status.GODKJENT, erPublisert = false, revisjonAvSistPublisert = false)
        val ok = BEGREP_3.copy(id = "id3-3", versjonsnr = SemVer(2, 10, 0), revisjonAv = "id3-1", status = Status.UTKAST, erPublisert = false, revisjonAvSistPublisert = true)

        whenever(conceptRepository.findById("id3-2"))
            .thenReturn(Optional.of(invalid.toDBO()))
        whenever(conceptRepository.findById("id3-3"))
            .thenReturn(Optional.of(ok.toDBO()))
        whenever(conceptRepository.getByOriginaltBegrep("id3"))
            .thenReturn(listOf(BEGREP_3.toDBO(), newPublished.toDBO()))

        val resultInvalid = conceptService.getConceptById("id3-2")
        val resultOk = conceptService.getConceptById("id3-3")

        assertEquals(invalid, resultInvalid)
        assertEquals(ok, resultOk)
    }

}

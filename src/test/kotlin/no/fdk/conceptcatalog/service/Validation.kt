package no.fdk.conceptcatalog.service

import no.fdk.conceptcatalog.configuration.ApplicationProperties
import no.fdk.conceptcatalog.configuration.JacksonConfigurer
import no.fdk.conceptcatalog.elastic.CurrentConceptRepository
import no.fdk.conceptcatalog.model.SemVer
import no.fdk.conceptcatalog.model.toEntity
import no.fdk.conceptcatalog.repository.ConceptRepository
import no.fdk.conceptcatalog.utils.BEGREP_5
import no.fdk.conceptcatalog.utils.toDBO
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse

@Tag("unit")
class Validation {
    private val conceptRepository: ConceptRepository = mock()
    private val conceptSearch: ConceptSearchService = mock()
    private val currentConceptRepository: CurrentConceptRepository = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val conceptPublisher: ConceptPublisher = mock()
    private val historyService: HistoryService = mock()

    private val conceptService =
        ConceptService(
            conceptRepository,
            conceptSearch,
            currentConceptRepository,
            applicationProperties,
            conceptPublisher,
            historyService,
            JacksonConfigurer().objectMapper(),
        )

    @Test
    fun `Is valid when any definition is defined for the concept`() {
        whenever(conceptRepository.findByOriginaltBegrep("id5"))
            .thenReturn(listOf(BEGREP_5.toDBO().toEntity()))

        val validVersion = BEGREP_5.copy(versjonsnr = SemVer(1, 0, 1))

        assertFalse { conceptService.isPublishedAndNotValid(validVersion) }
        assertFalse {
            conceptService.isPublishedAndNotValid(
                validVersion.copy(definisjonForAllmennheten = BEGREP_5.definisjon, definisjon = null),
            )
        }
        assertFalse {
            conceptService.isPublishedAndNotValid(
                validVersion.copy(definisjonForSpesialister = BEGREP_5.definisjon, definisjon = null),
            )
        }
    }
}

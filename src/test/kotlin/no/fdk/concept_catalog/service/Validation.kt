package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.elastic.ConceptSearchRepository
import no.fdk.concept_catalog.elastic.CurrentConceptRepository
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.utils.BEGREP_5
import no.fdk.concept_catalog.utils.toDBO
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.mongodb.core.MongoOperations
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("unit")
class Validation {
    private val conceptRepository: ConceptRepository = mock()
    private val conceptSearch: ConceptSearchService = mock()
    private val conceptSearchRepository: ConceptSearchRepository = mock()
    private val currentConceptRepository: CurrentConceptRepository = mock()
    private val mongoOperations: MongoOperations = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val conceptPublisher: ConceptPublisher = mock()
    private val historyService: HistoryService = mock()

    private val conceptService = ConceptService(
        conceptRepository, conceptSearch, conceptSearchRepository, currentConceptRepository, mongoOperations, applicationProperties, conceptPublisher, historyService, JacksonConfigurer().objectMapper())

    @Test
    fun `New non draft concepts has higher version than what is published`() {
        whenever(conceptRepository.getByOriginaltBegrep("id5"))
            .thenReturn(listOf(BEGREP_5, BEGREP_5.copy(id = "id7", versjonsnr = SemVer(12,10, 0)), BEGREP_5.copy(id = "id6", versjonsnr = SemVer(9, 9, 1))).map { it.toDBO() })

        assertFalse { conceptService.isPublishedAndNotValid(BEGREP_5.copy(id = "id8", versjonsnr = SemVer(12, 10, 1))) }
        assertFalse { conceptService.isPublishedAndNotValid(BEGREP_5.copy(id = "id8", versjonsnr = SemVer(12, 11, 0))) }
        assertFalse { conceptService.isPublishedAndNotValid(BEGREP_5.copy(id = "id8", versjonsnr = SemVer(245, 10, 0))) }

        assertTrue { conceptService.isPublishedAndNotValid(BEGREP_5.copy(id = "id8", versjonsnr = SemVer(12, 9, 95))) }
        assertTrue { conceptService.isPublishedAndNotValid(BEGREP_5.copy(id = "id8", versjonsnr = SemVer(11, 100, 99))) }
        assertTrue { conceptService.isPublishedAndNotValid(BEGREP_5.copy(id = "id8", versjonsnr = SemVer(9, 10, 1))) }
    }

}

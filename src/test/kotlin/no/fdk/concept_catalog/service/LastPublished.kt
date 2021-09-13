package no.fdk.concept_catalog.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.utils.BEGREP_3
import no.fdk.concept_catalog.utils.BEGREP_4
import no.fdk.concept_catalog.utils.BEGREP_5
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoOperations
import kotlin.test.assertTrue

@Tag("unit")
class LastPublished {
    private val conceptRepository: ConceptRepository = mock()
    private val mongoOperations: MongoOperations = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val conceptPublisher: ConceptPublisher = mock()

    private val conceptService = ConceptService(
        conceptRepository, mongoOperations, applicationProperties, conceptPublisher, JacksonConfigurer().objectMapper())

    @Test
    fun `Able to get a list with the highest version of concepts for a publisher`() {
        whenever(conceptRepository.getBegrepByAnsvarligVirksomhetIdAndStatus("111222333", Status.PUBLISERT))
            .thenReturn(listOf(BEGREP_3, BEGREP_4, BEGREP_3.copy(id = "id3-2", versjonsnr = SemVer(2, 10, 0), revisjonAv = "id3-1"),
                BEGREP_3.copy(id = "id3-1", versjonsnr = SemVer(1, 9, 1), revisjonAv = "id3"), BEGREP_5,
                BEGREP_4.copy(id = "id4-1", versjonsnr = SemVer(1, 0, 1), revisjonAv = "id4"),
                BEGREP_4.copy(id = "id4-2", versjonsnr = SemVer(3, 0, 0), revisjonAv = "id4-1"),
                BEGREP_5.copy(id = "id5-1", versjonsnr = SemVer(9, 9, 1), revisjonAv = "id5"),
                BEGREP_5.copy(id = "id5-2", versjonsnr = SemVer(12, 10, 0), revisjonAv = "id5-1")))

        val result = conceptService.getLastPublishedForOrganization("111222333").map { it.id }

        assertTrue { result.size == 3 }
        assertTrue { result.contains("id3-2") }
        assertTrue { result.contains("id4-2") }
        assertTrue { result.contains("id5-2") }
    }

}

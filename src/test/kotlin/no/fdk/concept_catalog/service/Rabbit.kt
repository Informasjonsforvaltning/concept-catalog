package no.fdk.concept_catalog.service

import com.nhaarman.mockitokotlin2.*
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.configuration.JacksonConfigurer
import no.fdk.concept_catalog.model.Virksomhet
import no.fdk.concept_catalog.repository.ConceptRepository
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.toDBO
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.data.mongodb.core.MongoOperations
import kotlin.test.assertEquals

@Tag("unit")
class Rabbit {
    private val conceptRepository: ConceptRepository = mock()
    private val mongoOperations: MongoOperations = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val conceptPublisher: ConceptPublisher = mock()

    private val conceptService = ConceptService(
        conceptRepository, mongoOperations, applicationProperties, conceptPublisher, JacksonConfigurer().objectMapper())

    @Test
    fun `Publish collection when first concept is created`() {
        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptRepository.countBegrepByAnsvarligVirksomhetId("123456789"))
            .thenReturn(0L)
        whenever(conceptRepository.save(any())).thenReturn(BEGREP_0.toDBO())

        conceptService.createConcept(BEGREP_0, "user_id")

        argumentCaptor<String, String>().apply {
            verify(conceptPublisher, times(1)).sendNewDataSource(first.capture(), second.capture())
            assertEquals("123456789", first.firstValue)
            assertEquals("https://concept-catalog.fellesdatakatalog.digdir.no/collections/123456789", second.firstValue)
        }
    }

    @Test
    fun `Only publish collection for publishers with no concepts in db`() {
        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptRepository.countBegrepByAnsvarligVirksomhetId("123456789"))
            .thenReturn(0L)
        whenever(conceptRepository.countBegrepByAnsvarligVirksomhetId("111222333"))
            .thenReturn(0L)
        whenever(conceptRepository.countBegrepByAnsvarligVirksomhetId("444555666"))
            .thenReturn(5L)
        whenever(conceptRepository.save(any())).thenReturn(BEGREP_0.toDBO())

        conceptService.createConcepts(listOf(BEGREP_0, BEGREP_0.copy(ansvarligVirksomhet = Virksomhet(id = "111222333")),
            BEGREP_0.copy(ansvarligVirksomhet = Virksomhet(id = "444555666"))), "user_id")

        argumentCaptor<String, String>().apply {
            verify(conceptPublisher, times(2)).sendNewDataSource(first.capture(), second.capture())
            assertEquals("123456789", first.firstValue)
            assertEquals("111222333", first.secondValue)
            assertEquals("https://concept-catalog.fellesdatakatalog.digdir.no/collections/123456789", second.firstValue)
            assertEquals("https://concept-catalog.fellesdatakatalog.digdir.no/collections/111222333", second.secondValue)
        }
    }

}

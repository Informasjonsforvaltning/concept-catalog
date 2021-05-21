package no.fdk.concept_catalog.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.Status
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_3
import no.fdk.concept_catalog.utils.BEGREP_4
import no.fdk.concept_catalog.utils.TestResponseReader
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class SkosApNo {
    private val conceptService: ConceptService = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val skosApNo = SkosApNoModelService(conceptService, applicationProperties)

    @Test
    fun `Single Collection`() {
        val expected = TestResponseReader().parseTurtleFile("collection_1.ttl")

        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://registrering-begrep.fellesdatakatalog.brreg.no")
        whenever(conceptService.getConceptsForOrganization("111222333", Status.PUBLISERT))
            .thenReturn(listOf(BEGREP_3, BEGREP_4))

        val model = skosApNo.buildModelForPublishersCollection("111222333")
        assertTrue { expected.isIsomorphicWith(model) }
    }

    @Test
    fun `All Collections`() {
        val expected = TestResponseReader().parseTurtleFile("all_collections.ttl")

        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://registrering-begrep.fellesdatakatalog.brreg.no")
        whenever(conceptService.getAllPublisherIds())
            .thenReturn(listOf("123456789", "111222333"))
        whenever(conceptService.getConceptsForOrganization("123456789", Status.PUBLISERT))
            .thenReturn(listOf(BEGREP_0))
        whenever(conceptService.getConceptsForOrganization("111222333", Status.PUBLISERT))
            .thenReturn(listOf(BEGREP_3, BEGREP_4))

        val model = skosApNo.buildModelForAllCollections()
        assertTrue { expected.isIsomorphicWith(model) }
    }

}

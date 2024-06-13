package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.Kildebeskrivelse
import no.fdk.concept_catalog.model.URITekst
import no.fdk.concept_catalog.rdf.EUVOC
import no.fdk.concept_catalog.rdf.SKOSNO
import no.fdk.concept_catalog.utils.*
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.RDFSyntax.RDF
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.slf4j.LoggerFactory
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

private val logger = LoggerFactory.getLogger(SkosApNo::class.java)

@Tag("unit")
class SkosApNo {
    private val conceptService: ConceptService = mock()
    private val applicationProperties: ApplicationProperties = mock()
    private val skosApNo = SkosApNoModelService(conceptService, applicationProperties)

    @Test
    fun `Single Collection`() {
        val expected = TestResponseReader().parseTurtleFile("collection_1.ttl")

        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(applicationProperties.adminServiceUri)
            .thenReturn("https://catalog-admin-service.fellesdatakatalog.digdir.no")
        whenever(conceptService.getLastPublishedForOrganization("111222333"))
            .thenReturn(listOf(BEGREP_3, BEGREP_4))

        val model = skosApNo.buildModelForPublishersCollection("111222333")
        assertTrue { checkIfIsomorphicAndPrintDiff(model, expected, "single collection", logger) }
    }

    @Test
    fun `All Collections`() {
        val expected = TestResponseReader().parseTurtleFile("all_collections.ttl")

        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(applicationProperties.adminServiceUri)
            .thenReturn("https://catalog-admin-service.fellesdatakatalog.digdir.no")
        whenever(conceptService.getAllPublisherIds())
            .thenReturn(listOf("123456789", "111222333"))
        whenever(conceptService.getLastPublishedForOrganization("123456789"))
            .thenReturn(listOf(BEGREP_0))
        whenever(conceptService.getLastPublishedForOrganization("111222333"))
            .thenReturn(listOf(BEGREP_3, BEGREP_4))

        val model = skosApNo.buildModelForAllCollections()
        assertTrue { checkIfIsomorphicAndPrintDiff(model, expected, "all collections", logger) }
    }

    @Test
    fun `isValidURI returns false on invalid source URI`() {
        var testString: String? = null
        assertFalse { testString.isValidURI() }

        testString = ""
        assertFalse { testString.isValidURI() }

        testString = "http:/not a uri"
        assertFalse { testString.isValidURI() }

        testString = ">noturi."
        assertFalse { testString.isValidURI() }
    }

    @Test
    fun `isValidURI returns true on valid URI`() {
        var testString: String? = "https://testdirektoratet.no"
        assertTrue { testString.isValidURI() }

        testString = "testdirektoratet.no/konsept/katalog"
        assertTrue { testString.isValidURI() }
    }

    @Test
    fun `Handles null and blank uri and label in omfang`() {
        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = "", tekst = "")))
        val modelBlankURIandLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet.id, BEGREP_6.id!!) }
        val omfangBlankURIandLabel = modelBlankURIandLabel.listObjectsOfProperty(SKOSNO.valueRange).toList()
        assertTrue { omfangBlankURIandLabel.isEmpty() }

        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = null, tekst = null)))
        val modelNullURIandLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet.id, BEGREP_6.id!!) }
        val omfangNullURIandLabel = modelNullURIandLabel.listObjectsOfProperty(SKOSNO.valueRange).toList()
        assertTrue { omfangNullURIandLabel.isEmpty() }
    }

    @Test
    fun `Throws exception on nonexistent publisher id`() {
        val nonexistentPublisherId = "000000000"
        assertThrows<ResponseStatusException> { skosApNo.buildModelForConcept(nonexistentPublisherId, BEGREP_6.id!!) }
    }

    @Test
    fun `Handles Concept with ForholdTilKildeEnum set to SITATFRAKILDE` () {
        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE, kilde = listOf(URITekst(uri = "https://valid.uri.no", tekst = "Testdirektoratet"))))))
        assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet.id, BEGREP_6.id!!) }
    }

    @Test
    fun `Handles Concept with invalid source URI` () {
        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE, kilde = listOf(URITekst(uri = "https://an invalid uri", tekst = "Testdirektoratet"))))))
        val modelInvalidURI = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet.id, BEGREP_6.id!!) }

        val sourceNullURI = modelInvalidURI.listObjectsOfProperty(EUVOC.xlDefinition).toList().first().asResource().getProperty(DCTerms.source)
        assertTrue { sourceNullURI.`object`.isLiteral }
        assertEquals("Testdirektoratet", sourceNullURI.literal.string)
    }
}

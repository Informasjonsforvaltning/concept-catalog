package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.Kildebeskrivelse
import no.fdk.concept_catalog.model.URITekst
import no.fdk.concept_catalog.utils.*
import no.norge.data.skos_ap_no.concept.builder.SKOSNO
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDFS
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertFalse
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
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptService.getLastPublishedForOrganization("111222333"))
            .thenReturn(listOf(BEGREP_3, BEGREP_4))

        val model = skosApNo.buildModelForPublishersCollection("111222333")
        assertTrue { expected.isIsomorphicWith(model) }
    }

    @Test
    fun `All Collections`() {
        val expected = TestResponseReader().parseTurtleFile("all_collections.ttl")

        whenever(applicationProperties.collectionBaseUri)
            .thenReturn("https://concept-catalog.fellesdatakatalog.digdir.no")
        whenever(conceptService.getAllPublisherIds())
            .thenReturn(listOf("123456789", "111222333"))
        whenever(conceptService.getLastPublishedForOrganization("123456789"))
            .thenReturn(listOf(BEGREP_0))
        whenever(conceptService.getLastPublishedForOrganization("111222333"))
            .thenReturn(listOf(BEGREP_3, BEGREP_4))

        val model = skosApNo.buildModelForAllCollections()
        assertTrue { expected.isIsomorphicWith(model) }
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
    fun `Handles null and blank URI in omfang`() {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = "", tekst = "Testdirektoratet")))
        val modelBlankURI = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val omfangBlankURI = modelBlankURI.listObjectsOfProperty(SKOSNO.omfang).toList().first().asResource()
        assertFalse { omfangBlankURI.hasProperty(RDFS.seeAlso) }
        assertTrue { omfangBlankURI.hasProperty(RDFS.label) }

        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = null, tekst = "Testdirektoratet")))
        val modelNullURI = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val omfangNullURI = modelNullURI.listObjectsOfProperty(SKOSNO.omfang).toList().first().asResource()
        assertFalse { omfangNullURI.hasProperty(RDFS.seeAlso) }
        assertTrue { omfangNullURI.hasProperty(RDFS.label) }
    }

    @Test
    fun `Handles null and blank label in omfang`() {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = "https://valid.uri.no", tekst = "")))
        val modelBlankLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val omfangBlankLabel = modelBlankLabel.listObjectsOfProperty(SKOSNO.omfang).toList().first().asResource()
        assertFalse { omfangBlankLabel.hasProperty(RDFS.label) }
        assertTrue { omfangBlankLabel.hasProperty(RDFS.seeAlso) }

        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = "https://valid.uri.no", tekst = null)))
        val modelNullLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val omfangNullLabel = modelNullLabel.listObjectsOfProperty(SKOSNO.omfang).toList().first().asResource()
        assertFalse { omfangNullLabel.hasProperty(RDFS.label) }
        assertTrue { omfangNullLabel.hasProperty(RDFS.seeAlso) }
    }

    @Test
    fun `Handles null and blank uri and label in omfang`() {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = "", tekst = "")))
        val modelBlankURIandLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val omfangBlankURIandLabel = modelBlankURIandLabel.listObjectsOfProperty(SKOSNO.omfang).toList()
        assertTrue { omfangBlankURIandLabel.isEmpty() }

        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(omfang = URITekst(uri = null, tekst = null)))
        val modelNullURIandLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val omfangNullURIandLabel = modelNullURIandLabel.listObjectsOfProperty(SKOSNO.omfang).toList()
        assertTrue { omfangNullURIandLabel.isEmpty() }
    }

    @Test
    fun `Handles null and blank URI in source`() {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = null, kilde = listOf(URITekst(uri = "", tekst = "Testdirektoratet"))))))
        val modelBlankURI = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val sourceBlankURI = modelBlankURI.listObjectsOfProperty(SKOSNO.definisjon).toList().first().asResource().getPropertyResourceValue(DCTerms.source)
        assertFalse { sourceBlankURI.hasProperty(RDFS.seeAlso) }
        assertTrue { sourceBlankURI.hasProperty(RDFS.label) }

        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = null, kilde = listOf(URITekst(uri = null, tekst = "Testdirektoratet"))))))
        val modelNullURI = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val sourceNullURI = modelNullURI.listObjectsOfProperty(SKOSNO.definisjon).toList().first().asResource().getPropertyResourceValue(DCTerms.source)
        assertFalse { sourceNullURI.hasProperty(RDFS.seeAlso) }
        assertTrue { sourceNullURI.hasProperty(RDFS.label) }
    }

    @Test
    fun `Handles null and blank label in source`() {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = null, kilde = listOf(URITekst(uri = "https://valid.uri.no", tekst = ""))))))
        val modelBlankLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val sourceBlankLabel = modelBlankLabel.listObjectsOfProperty(SKOSNO.definisjon).toList().first().asResource().getPropertyResourceValue(DCTerms.source)
        assertFalse { sourceBlankLabel.hasProperty(RDFS.label) }
        assertTrue { sourceBlankLabel.hasProperty(RDFS.seeAlso) }

        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = null, kilde = listOf(URITekst(uri = "https://valid.uri.no", tekst = null))))))
        val modelNullLabel = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
        val sourceNullLabel = modelNullLabel.listObjectsOfProperty(SKOSNO.definisjon).toList().first().asResource().getPropertyResourceValue(DCTerms.source)
        assertFalse { sourceNullLabel.hasProperty(RDFS.label) }
        assertTrue { sourceNullLabel.hasProperty(RDFS.seeAlso) }
    }

    @Test
    fun `Throws exception on nonexistent publisher id`() {
        val nonexistentPublisherId = "000000000"
        assertThrows<ResponseStatusException> { skosApNo.buildModelForConcept(nonexistentPublisherId, BEGREP_6.id!!) }
    }

    @Test
    fun `Handles Concept with ForholdTilKildeEnum set to SITATFRAKILDE` () {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE, kilde = listOf(URITekst(uri = "https://valid.uri.no", tekst = "Testdirektoratet"))))))
        assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }
    }

    @Test
    fun `Handles Concept with invalid source URI` () {
        whenever(conceptService.getLastPublished(BEGREP_6.id))
            .thenReturn(BEGREP_6.copy(definisjon = BEGREP_6.definisjon?.copy(kildebeskrivelse = Kildebeskrivelse(forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE, kilde = listOf(URITekst(uri = "https://an invalid uri", tekst = "Testdirektoratet"))))))
        val modelInvalidURI = assertDoesNotThrow { skosApNo.buildModelForConcept(BEGREP_6.ansvarligVirksomhet!!.id!!, BEGREP_6.id!!) }

        val sourceNullURI = modelInvalidURI.listObjectsOfProperty(SKOSNO.definisjon).toList().first().asResource().getPropertyResourceValue(DCTerms.source)
        assertFalse { sourceNullURI.hasProperty(RDFS.seeAlso) }
        assertTrue { sourceNullURI.hasProperty(RDFS.label) }
    }
}

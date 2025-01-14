package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.URITekst
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.SKOS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.nio.charset.StandardCharsets

@Tag("unit")
class SkosApNoImportTests {

    @Test
    fun `should extract list of concepts`() {
        val model = readModel("import_concept.ttl")

        val concepts = model.extractBegreper("catalogId")

        assertEquals(1, concepts.size)

        concepts.first().let { concept ->
            assertNotNull(concept.versjonsnr)
            assertNotNull(concept.statusURI)

            assertNotNull(concept.anbefaltTerm)
            concept.tillattTerm?.let { assertFalse(it.isEmpty()) }
            concept.frarådetTerm?.let { assertFalse(it.isEmpty()) }

            assertNotNull(concept.definisjon)
            assertNotNull(concept.definisjonForAllmennheten)
            assertNotNull(concept.definisjonForSpesialister)

            concept.merknad?.let { assertFalse(it.isEmpty()) }
            concept.eksempel?.let { assertFalse(it.isEmpty()) }
        }
    }

    @Test
    fun `should extract versjonsnr`() {
        val model = readModel("import_concept.ttl")

        model.listResourcesWithProperty(OWL.versionInfo)
            .toList()
            .map { it.extractVersjonr() }
            .firstNotNullOf {
                assertEquals(SemVer(1, 0, 0), it)
            }
    }

    @Test
    fun `should extract statusUri`() {
        val model = readModel("import_concept.ttl")

        model.listResourcesWithProperty(EUVOC.status)
            .toList()
            .map { it.extractStatusUri() }
            .firstNotNullOf {
                assertEquals("http://publications.europa.eu/resource/authority/concept-status/CURRENT", it)
            }
    }

    @Test
    fun `should extract anbefaltTerm`() {
        val model = readModel("import_concept.ttl")

        val terms = model.listResourcesWithProperty(SKOS.prefLabel)
            .toList()
            .map { it.extractAnbefaltTerm() }

        assertEquals(1, terms.size)

        terms.first()?.navn?.let { localizedTerms ->
            assertEquals(2, localizedTerms.size)
            assertTrue(localizedTerms.containsKey("nb"))
            assertEquals("anbefaltTerm", localizedTerms["nb"])
            assertTrue(localizedTerms.containsKey("en"))
            assertEquals("recommendedTerm", localizedTerms["en"])
        }
    }

    @Test
    fun `should extract tillattTerm`() {
        val model = readModel("import_concept.ttl")

        val terms = model.listResourcesWithProperty(SKOS.altLabel)
            .toList()
            .map { it.extractTillattTerm() }

        assertEquals(1, terms.size)

        terms.first()?.let { localizedTerms ->
            assertTrue(localizedTerms.containsKey("nn"))
            assertEquals(localizedTerms.getValue("nn").toSet(), setOf("tillattTerm", "tillattTerm2"))
        }
    }

    @Test
    fun `should extract frarådetTerm`() {
        val model = readModel("import_concept.ttl")

        val terms = model.listResourcesWithProperty(SKOS.hiddenLabel)
            .toList()
            .map { it.extractFrarådetTerm() }

        assertEquals(1, terms.size)

        terms.first()?.let { localizedTerms ->
            assertTrue(localizedTerms.containsKey("nb"))
            assertEquals(localizedTerms.getValue("nb").toSet(), setOf("fraraadetTerm", "fraraadetTerm2", "Lorem ipsum"))
        }
    }

    @Test
    fun `should extract definisjon`() {
        val model = readModel("import_concept.ttl")

        val definitions = model.listResourcesWithProperty(EUVOC.xlDefinition)
            .toList()
            .map { it.extractDefinisjon() }

        assertEquals(1, definitions.size)

        definitions.first()?.let {
            it.tekst?.let { text ->
                assertEquals(2, text.size)
                assertTrue(text.containsKey("nb"))
                assertEquals("definisjon", text["nb"])
                assertTrue(text.containsKey("nb"))
                assertEquals("definition", text["en"])
            }

            it.kildebeskrivelse?.let { sourceDescription ->
                assertEquals(ForholdTilKildeEnum.EGENDEFINERT, sourceDescription.forholdTilKilde)

                sourceDescription.kilde?.let { source ->
                    assertEquals(2, source.size)
                    assertEquals(URITekst(tekst = "kap14"), source.first())
                    assertEquals(
                        URITekst(uri = "https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14"),
                        source.last()
                    )
                }
            }
        }
    }

    @Test
    fun `should extract definisjonForAllmennheten`() {
        val model = readModel("import_concept.ttl")

        val definitions = model.listResourcesWithProperty(EUVOC.xlDefinition)
            .toList()
            .map { it.extractDefinisjonForAllmennheten() }

        assertEquals(1, definitions.size)

        definitions.first()?.let {
            it.tekst?.let { text ->
                assertEquals(1, text.size)
                assertTrue(text.containsKey("nb"))
                assertEquals("definisjon for allmennheten", text["nb"])
            }

            it.kildebeskrivelse?.let { sourceDescription ->
                assertEquals(ForholdTilKildeEnum.SITATFRAKILDE, sourceDescription.forholdTilKilde)
            }
        }
    }

    @Test
    fun `should extract definisjonForSpesialister`() {
        val model = readModel("import_concept.ttl")

        val definitions = model.listResourcesWithProperty(EUVOC.xlDefinition)
            .toList()
            .map { it.extractDefinisjonForSpesialister() }

        assertEquals(1, definitions.size)

        definitions.first()?.let {
            it.tekst?.let { text ->
                assertEquals(1, text.size)
                assertTrue(text.containsKey("nb"))
                assertEquals("definisjon for spesialister", text["nb"])
            }

            it.kildebeskrivelse?.let { sourceDescription ->
                assertEquals(ForholdTilKildeEnum.BASERTPAAKILDE, sourceDescription.forholdTilKilde)
            }
        }
    }

    @Test
    fun `should extract merknad`() {
        val model = readModel("import_concept.ttl")

        val notes = model.listResourcesWithProperty(SKOS.scopeNote)
            .toList()
            .map { it.extractMerknad() }

        assertEquals(1, notes.size)

        notes.first()?.let { localizedNote ->
            assertEquals(2, localizedNote.size)
            assertTrue(localizedNote.containsKey("nb"))
            assertEquals("merknad", localizedNote["nb"])
            assertTrue(localizedNote.containsKey("nn"))
            assertEquals("merknad", localizedNote["nn"])
        }
    }

    @Test
    fun `should extract eksempel`() {
        val model = readModel("import_concept.ttl")

        val notes = model.listResourcesWithProperty(SKOS.example)
            .toList()
            .map { it.extractEksempel() }

        assertEquals(1, notes.size)

        notes.first()?.let { localizedNote ->
            assertEquals(2, localizedNote.size)
            assertTrue(localizedNote.containsKey("nb"))
            assertEquals("eksempel", localizedNote["nb"])
            assertTrue(localizedNote.containsKey("nn"))
            assertEquals("eksempel", localizedNote["nn"])
        }
    }

    private fun readModel(file: String): Model {
        val turtle = javaClass.classLoader.getResourceAsStream(file)
            ?.let { String(it.readAllBytes(), StandardCharsets.UTF_8) }

        val model = ModelFactory.createDefaultModel()
        model.read(StringReader(turtle!!), "http://example.com", Lang.TURTLE.name)

        return model
    }
}

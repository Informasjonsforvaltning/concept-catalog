package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.URITekst
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.SKOS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.nio.charset.StandardCharsets

@Tag("unit")
class SkosApNoImportTests {

    @Test
    fun `should extract list of concepts`() {
        val model = readModel("concept.ttl")

        val concepts = model.extractBegreper("catalogId")

        assertEquals(1, concepts.size)
    }

    @Test
    fun `should extract statusUri`() {
        val model = readModel("concept.ttl")

        model.listResourcesWithProperty(EUVOC.status)
            .toList()
            .map { it.extractStatusUri() }
            .firstNotNullOf {
                assertEquals("http://publications.europa.eu/resource/authority/concept-status/CURRENT", it)
            }
    }

    @Test
    fun `should extract anbefaltTerm`() {
        val model = readModel("concept.ttl")

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
        val model = readModel("concept.ttl")

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
        val model = readModel("concept.ttl")

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
        val model = readModel("concept.ttl")

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
        val model = readModel("concept.ttl")

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
        val model = readModel("concept.ttl")

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

    private fun readModel(file: String): Model {
        val turtle = javaClass.classLoader.getResourceAsStream(file)
            ?.let { String(it.readAllBytes(), StandardCharsets.UTF_8) }

        val model = ModelFactory.createDefaultModel()
        model.read(StringReader(turtle!!), "http://example.com", Lang.TURTLE.name)

        return model
    }
}

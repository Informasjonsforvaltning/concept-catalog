package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.URITekst
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@Tag("unit")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkosApNoImportTests {

    lateinit var model: Model

    @BeforeAll
    fun setup() {
        val turtle = String(
            javaClass.classLoader.getResourceAsStream("import_concept.ttl")!!.readAllBytes(),
            StandardCharsets.UTF_8
        )

        model = ModelFactory.createDefaultModel()
        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
    }

    @Test
    fun `should extract list of concepts`() {
        val concepts = model.extractBegreper("catalogId")

        assertEquals(1, concepts.size)

        concepts.first().let {
            assertNotNull(it.versjonsnr)
            assertNotNull(it.statusURI)

            assertNotNull(it.anbefaltTerm)
            assertFalse(it.tillattTerm!!.isEmpty())
            assertFalse(it.frarådetTerm!!.isEmpty())

            assertNotNull(it.definisjon)
            assertNotNull(it.definisjonForAllmennheten)
            assertNotNull(it.definisjonForSpesialister)

            assertFalse(it.merknad!!.isEmpty())
            assertFalse(it.eksempel!!.isEmpty())
            assertFalse(it.fagområde!!.isEmpty())
            assertFalse(it.fagområdeKoder!!.isEmpty())
            assertNotNull(it.omfang)
            assertNotNull(it.gyldigFom)
            assertNotNull(it.gyldigTom)
            assertNotNull(it.seOgså!!.isEmpty())
            assertFalse(it.erstattesAv!!.isEmpty())
            assertNotNull(it.kontaktpunkt)
            assertFalse(it.begrepsRelasjon!!.isEmpty())
        }
    }

    @Test
    fun `should extract versjonsnr`() {
        val concepts = model.extractBegreper("catalogId")

        assertEquals(SemVer(1, 0, 0), concepts.first().versjonsnr)
    }

    @Test
    fun `should extract statusUri`() {
        val concepts = model.extractBegreper("catalogId")

        assertEquals(
            "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
            concepts.first().statusURI
        )
    }

    @Test
    fun `should extract anbefaltTerm`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().anbefaltTerm!!.navn.let {
            assertEquals(2, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals("anbefaltTerm", it["nb"])
            assertTrue(it.containsKey("en"))
            assertEquals("recommendedTerm", it["en"])
        }
    }

    @Test
    fun `should extract tillattTerm`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().tillattTerm!!.let {
            assertTrue(it.containsKey("nn"))
            assertEquals(it.getValue("nn").toSet(), setOf("tillattTerm", "tillattTerm2"))
        }
    }

    @Test
    fun `should extract frarådetTerm`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().frarådetTerm!!.let {
            assertTrue(it.containsKey("nb"))
            assertEquals(it.getValue("nb").toSet(), setOf("fraraadetTerm", "fraraadetTerm2", "Lorem ipsum"))
        }
    }

    @Test
    fun `should extract definisjon`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().definisjon!!.let {
            it.tekst.let { text ->
                assertEquals(2, text!!.size)
                assertTrue(text.containsKey("nb"))
                assertEquals("definisjon", text["nb"])
                assertTrue(text.containsKey("nb"))
                assertEquals("definition", text["en"])
            }

            it.kildebeskrivelse.let { sourceDescription ->
                assertEquals(ForholdTilKildeEnum.EGENDEFINERT, sourceDescription!!.forholdTilKilde)

                sourceDescription.kilde.let { source ->
                    assertEquals(2, source!!.size)
                    assertEquals(
                        setOf(
                            URITekst(tekst = "kap14"),
                            URITekst(uri = "https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14")
                        ),
                        source.toSet()
                    )
                }
            }
        }
    }

    @Test
    fun `should extract definisjonForAllmennheten`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().definisjonForAllmennheten!!.let {
            it.tekst.let { text ->
                assertEquals(1, text!!.size)
                assertTrue(text.containsKey("nb"))
                assertEquals("definisjon for allmennheten", text["nb"])
            }

            assertEquals(ForholdTilKildeEnum.SITATFRAKILDE, it.kildebeskrivelse!!.forholdTilKilde)
        }
    }

    @Test
    fun `should extract definisjonForSpesialister`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().definisjonForSpesialister!!.let {
            it.tekst.let { text ->
                assertEquals(1, text!!.size)
                assertTrue(text.containsKey("nb"))
                assertEquals("definisjon for spesialister", text["nb"])
            }

            assertEquals(ForholdTilKildeEnum.BASERTPAAKILDE, it.kildebeskrivelse!!.forholdTilKilde)
        }
    }

    @Test
    fun `should extract merknad`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().merknad!!.let {
            assertEquals(2, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals("merknad", it["nb"])
            assertTrue(it.containsKey("nn"))
            assertEquals("merknad", it["nn"])
        }
    }

    @Test
    fun `should extract eksempel`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().eksempel!!.let {
            assertEquals(2, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals("eksempel", it["nb"])
            assertTrue(it.containsKey("nn"))
            assertEquals("eksempel", it["nn"])
        }
    }

    @Test
    fun `should extract fagområde`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().fagområde!!.let {
            assertEquals(1, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals(it.getValue("nb").toSet(), setOf("fagområde"))
        }
    }

    @Test
    fun `should extract fagområdeKoder`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().fagområdeKoder!!.let {
            assertEquals(1, it.size)
            assertTrue(it.contains("https://example.com/subject"))
        }
    }

    @Test
    fun `should extract omfang`() {
        val concepts = model.extractBegreper("catalogId")

        assertEquals(URITekst("https://example.com/valueRange", "omfang"), concepts.first().omfang)
    }

    @Test
    fun `should extract gyldigFom`() {
        val concepts = model.extractBegreper("catalogId")

        assertEquals(LocalDate.of(2020, 12, 31), concepts.first().gyldigFom)
    }

    @Test
    fun `should extract gyldigTom`() {
        val concepts = model.extractBegreper("catalogId")

        assertEquals(LocalDate.of(2030, 12, 31), concepts.first().gyldigTom)
    }

    @Test
    fun `should extract seOgså`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().seOgså!!.let {
            assertEquals(1, it.size)
            assertTrue(it.contains("https://example.com/seeAlsoConcept"))
        }
    }

    @Test
    fun `should extract erstattesAv`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().erstattesAv!!.let {
            assertEquals(1, it.size)
            assertTrue(it.contains("https://example.com/isReplacedByConcept"))
        }
    }

    @Test
    fun `should extract kontaktpunkt`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().kontaktpunkt!!.let {
            assertEquals("organization@example.com", it.harEpost)
            assertEquals("+123-456-789", it.harTelefon)
        }
    }

    @Test
    fun `should extract begrepsRelasjon`() {
        val concepts = model.extractBegreper("catalogId")

        concepts.first().begrepsRelasjon!!.let { relations ->
            assertEquals(5, relations.size)

            relations.find { it.relasjon == "assosiativ" }!!
                .let { associative ->
                    assertEquals("https://example.com/topConcept", associative.relatertBegrep)

                    associative.beskrivelse!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "muliggjør")
                    }
                }

            relations.find { it.relasjon == "partitiv" && it.relasjonsType == "omfatter" }!!
                .let { partitive ->
                    assertEquals("https://example.com/partitiveConcept", partitive.relatertBegrep)

                    partitive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }

            relations.find { it.relasjon == "partitiv" && it.relasjonsType == "erDelAv" }!!
                .let { comprehensive ->
                    assertEquals("https://example.com/comprehensiveConcept", comprehensive.relatertBegrep)

                    comprehensive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }

            relations.find { it.relasjon == "generisk" && it.relasjonsType == "overordnet" }!!
                .let { comprehensive ->
                    assertEquals("https://example.com/genericConcept", comprehensive.relatertBegrep)

                    comprehensive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }

            relations.find { it.relasjon == "generisk" && it.relasjonsType == "underordnet" }!!
                .let { comprehensive ->
                    assertEquals("https://example.com/specificConcept", comprehensive.relatertBegrep)

                    comprehensive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }
        }
    }
}

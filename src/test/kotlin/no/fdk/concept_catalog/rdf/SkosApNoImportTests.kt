package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.ForholdTilKildeEnum
import no.fdk.concept_catalog.model.SemVer
import no.fdk.concept_catalog.model.URITekst
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.vocabulary.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate

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
            assertFalse(concept.tillattTerm!!.isEmpty())
            assertFalse(concept.frarådetTerm!!.isEmpty())

            assertNotNull(concept.definisjon)
            assertNotNull(concept.definisjonForAllmennheten)
            assertNotNull(concept.definisjonForSpesialister)

            assertFalse(concept.merknad!!.isEmpty())
            assertFalse(concept.eksempel!!.isEmpty())
            assertFalse(concept.fagområde!!.isEmpty())
            assertFalse(concept.fagområdeKoder!!.isEmpty())
            assertNotNull(concept.omfang)
            assertNotNull(concept.gyldigFom)
            assertNotNull(concept.gyldigTom)
            assertNotNull(concept.seOgså!!.isEmpty())
            assertFalse(concept.erstattesAv!!.isEmpty())
            assertNotNull(concept.kontaktpunkt)
        }
    }

    @Test
    fun `should extract versjonsnr`() {
        val model = readModel("import_concept.ttl")

        val versionInfo = model.listResourcesWithProperty(OWL.versionInfo)
            .toList()
            .first()
            .extractVersjonr()

        assertEquals(SemVer(1, 0, 0), versionInfo)
    }

    @Test
    fun `should extract statusUri`() {
        val model = readModel("import_concept.ttl")

        val status = model.listResourcesWithProperty(EUVOC.status)
            .toList()
            .first()
            .extractStatusUri()

        assertEquals("http://publications.europa.eu/resource/authority/concept-status/CURRENT", status)
    }

    @Test
    fun `should extract anbefaltTerm`() {
        val model = readModel("import_concept.ttl")

        val prefLabel = model.listResourcesWithProperty(SKOS.prefLabel)
            .toList()
            .first()
            .extractAnbefaltTerm()

        prefLabel!!.navn.let {
            assertEquals(2, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals("anbefaltTerm", it["nb"])
            assertTrue(it.containsKey("en"))
            assertEquals("recommendedTerm", it["en"])
        }
    }

    @Test
    fun `should extract tillattTerm`() {
        val model = readModel("import_concept.ttl")

        val altLabel = model.listResourcesWithProperty(SKOS.altLabel)
            .toList()
            .first()
            .extractTillattTerm()

        altLabel!!.let {
            assertTrue(it.containsKey("nn"))
            assertEquals(it.getValue("nn").toSet(), setOf("tillattTerm", "tillattTerm2"))
        }
    }

    @Test
    fun `should extract frarådetTerm`() {
        val model = readModel("import_concept.ttl")

        val hiddenLabel = model.listResourcesWithProperty(SKOS.hiddenLabel)
            .toList()
            .first()
            .extractFrarådetTerm()

        hiddenLabel!!.let {
            assertTrue(it.containsKey("nb"))
            assertEquals(it.getValue("nb").toSet(), setOf("fraraadetTerm", "fraraadetTerm2", "Lorem ipsum"))
        }
    }

    @Test
    fun `should extract definisjon`() {
        val model = readModel("import_concept.ttl")

        val xlDefinition = model.listResourcesWithProperty(EUVOC.xlDefinition)
            .toList()
            .first()
            .extractDefinisjon()

        xlDefinition!!.let {
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
        val model = readModel("import_concept.ttl")

        val xlDefinition = model.listResourcesWithProperty(EUVOC.xlDefinition)
            .toList()
            .first()
            .extractDefinisjonForAllmennheten()

        xlDefinition!!.let {
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
        val model = readModel("import_concept.ttl")

        val xlDefinition = model.listResourcesWithProperty(EUVOC.xlDefinition)
            .toList()
            .first()
            .extractDefinisjonForSpesialister()

        xlDefinition!!.let {
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
        val model = readModel("import_concept.ttl")

        val scopeNote = model.listResourcesWithProperty(SKOS.scopeNote)
            .toList()
            .first()
            .extractMerknad()

        scopeNote!!.let {
            assertEquals(2, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals("merknad", it["nb"])
            assertTrue(it.containsKey("nn"))
            assertEquals("merknad", it["nn"])
        }
    }

    @Test
    fun `should extract eksempel`() {
        val model = readModel("import_concept.ttl")

        val example = model.listResourcesWithProperty(SKOS.example)
            .toList()
            .first()
            .extractEksempel()

        example!!.let {
            assertEquals(2, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals("eksempel", it["nb"])
            assertTrue(it.containsKey("nn"))
            assertEquals("eksempel", it["nn"])
        }
    }

    @Test
    fun `should extract fagområde`() {
        val model = readModel("import_concept.ttl")

        val subject = model.listResourcesWithProperty(DCTerms.subject)
            .toList()
            .first()
            .extractFagområde()

        subject!!.let {
            assertEquals(1, it.size)
            assertTrue(it.containsKey("nb"))
            assertEquals(it.getValue("nb").toSet(), setOf("fagområde"))
        }
    }

    @Test
    fun `should extract fagområdeKoder`() {
        val model = readModel("import_concept.ttl")

        val subject = model.listResourcesWithProperty(DCTerms.subject)
            .toList()
            .first()
            .extractFagområdeKoder()

        subject!!.let {
            assertEquals(1, it.size)
            assertTrue(it.contains("https://example.com/subject"))
        }
    }

    @Test
    fun `should extract omfang`() {
        val model = readModel("import_concept.ttl")

        val valueRange = model.listResourcesWithProperty(SKOSNO.valueRange)
            .toList()
            .first()
            .extractOmfang()

        assertEquals(URITekst("https://example.com/valueRange", "omfang"), valueRange)
    }

    @Test
    fun `should extract gyldigFom`() {
        val model = readModel("import_concept.ttl")

        val startDate = model.listResourcesWithProperty(EUVOC.startDate)
            .toList()
            .first()
            .extractGyldigFom()

        assertEquals(LocalDate.of(2020, 12, 31), startDate)
    }

    @Test
    fun `should extract gyldigTom`() {
        val model = readModel("import_concept.ttl")

        val endDate = model.listResourcesWithProperty(EUVOC.endDate)
            .toList()
            .first()
            .extractGyldigTom()

        assertEquals(LocalDate.of(2030, 12, 31), endDate)
    }

    @Test
    fun `should extract seOgså`() {
        val model = readModel("import_concept.ttl")

        val seeAlso = model.listResourcesWithProperty(RDFS.seeAlso)
            .toList()
            .first()
            .extractSeOgså()

        seeAlso!!.let {
            assertEquals(1, it.size)
            assertTrue(it.contains("https://example.com/seeAlsoConcept"))
        }
    }

    @Test
    fun `should extract erstattesAv`() {
        val model = readModel("import_concept.ttl")

        val seeAlso = model.listResourcesWithProperty(DCTerms.isReplacedBy)
            .toList()
            .first()
            .extractErstattesAv()

        seeAlso!!.let {
            assertEquals(1, it.size)
            assertTrue(it.contains("https://example.com/isReplacedByConcept"))
        }
    }

    @Test
    fun `should extract kontaktpunkt`() {
        val model = readModel("import_concept.ttl")

        val contactPoint = model.listResourcesWithProperty(DCAT.contactPoint)
            .toList()
            .first()
            .extractKontaktPunkt()

        contactPoint!!.let {
            assertEquals("organization@example.com", it.harEpost)
            assertEquals("+123-456-789", it.harTelefon)
        }
    }

    private fun readModel(file: String): Model {
        val turtle = String(javaClass.classLoader.getResourceAsStream(file)!!.readAllBytes(), StandardCharsets.UTF_8)

        val model = ModelFactory.createDefaultModel()
        model.read(StringReader(turtle), "http://example.com", Lang.TURTLE.name)

        return model
    }
}

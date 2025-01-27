package no.fdk.concept_catalog.rdf

import no.fdk.concept_catalog.model.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.time.LocalDate

@Tag("unit")
class SkosApNoImportMapperTests {

    lateinit var model: Model

    @BeforeEach
    fun setup() {
        model = ModelFactory.createDefaultModel()
    }

    @Test
    fun `should extract anbefaltTerm`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(1, result.operations.size)

            result.operations.first().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/anbefaltTerm", it.path)

                assertTrue(it.value is Term)
                assertEquals(
                    Term(
                        mapOf(
                            "nb" to "anbefaltTerm",
                            "en" to "recommendedTerm"
                        )
                    ), it.value as Term
                )
            }
        }
    }

    @Test
    fun `should fail to extract anbefaltTerm`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(1, result.issues.size)

            result.issues.first().let {
                assertEquals(IssueType.ERROR, it.type)
                assertEquals("Missing skos:prefLabel", it.message)
            }
        }
    }

    @Test
    fun `should extract tillatTerm`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    skos:altLabel         "tillattTerm"@nb, "tillattTerm2"@nb .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/tillattTerm", it.path)

                assertTrue(it.value is Map<*, *>)
                val altLabels = (it.value as Map<*, *>)["nb"] as List<*>

                assertEquals(setOf("tillattTerm", "tillattTerm2"), altLabels.toSet())
            }
        }
    }

    @Test
    fun `should extract frarådetTerm`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    skos:hiddenLabel      "fraraadetTerm"@nb, "fraraadetTerm2"@nb .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/frarådetTerm", it.path)

                assertTrue(it.value is Map<*, *>)
                val hiddenLabels = (it.value as Map<*, *>)["nb"] as List<*>

                assertEquals(setOf("fraraadetTerm", "fraraadetTerm2"), hiddenLabels.toSet())
            }
        }
    }

    @Test
    fun `should extract versjonsnr`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix owl:   <http://www.w3.org/2002/07/owl#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    owl:versionInfo       "1.0.0" .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/versjonsnr", it.path)

                assertTrue(it.value is SemVer)
                assertEquals(SemVer(1, 0, 0), it.value as SemVer)
            }
        }
    }

    @Test
    fun `should fail to extract versjonsnr`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix owl:   <http://www.w3.org/2002/07/owl#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    owl:versionInfo       "1" .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(1, result.issues.size)

            result.issues.first().let {
                assertEquals(IssueType.ERROR, it.type)
                assertEquals("Invalid format for owl:versionInfo: 1", it.message)
            }
        }
    }

    @Test
    fun `should extract statusURI`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix euvoc: <http://publications.europa.eu/ontology/euvoc#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en ;
                    euvoc:status          <http://publications.europa.eu/resource/authority/concept-status/CURRENT> .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/statusURI", it.path)

                assertTrue(it.value is String)
                assertEquals(
                    "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
                    it.value as String
                )
            }
        }
    }

    @Test
    fun `should fail to extract statusURI`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix euvoc: <http://publications.europa.eu/ontology/euvoc#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:status          "invalid status" .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(1, result.issues.size)

            result.issues.first().let {
                assertEquals(IssueType.ERROR, it.type)
                assertEquals("Invalid URI for euvoc:status: invalid status", it.message)
            }
        }
    }

    @Test
    fun `should extract definisjon`() {
        val turtle = """
            @prefix rdf:                            <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:                           <http://www.w3.org/2004/02/skos/core#> .
            @prefix euvoc:                          <http://publications.europa.eu/ontology/euvoc#> .
            @prefix skosno:                         <https://data.norge.no/vocabulary/skosno#> .
            @prefix dct:                            <http://purl.org/dc/terms/> .
            @prefix relationship-with-source-type:  <https://data.norge.no/vocabulary/relationship-with-source-type#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon"@nb, "definition"@en ;
                            skosno:relationshipWithSource   relationship-with-source-type:self-composed ;
                            dct:source                      "kap14", <https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14> ;
                          ] .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/definisjon", it.path)

                assertTrue(it.value is Definisjon)
                val xlDefinition = it.value as Definisjon

                xlDefinition.tekst.let { text ->
                    assertEquals(2, text!!.size)
                    assertEquals("definisjon", text["nb"])
                    assertEquals("definition", text["en"])
                }

                xlDefinition.kildebeskrivelse.let { sourceDescription ->
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
    }

    @Test
    fun `should extract definisjonForAllmennheten`() {
        val turtle = """
            @prefix rdf:                            <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:                           <http://www.w3.org/2004/02/skos/core#> .
            @prefix euvoc:                          <http://publications.europa.eu/ontology/euvoc#> .
            @prefix skosno:                         <https://data.norge.no/vocabulary/skosno#> .
            @prefix dct:                            <http://purl.org/dc/terms/> .
            @prefix audience-type:                  <https://data.norge.no/vocabulary/audience-type#> .
            @prefix relationship-with-source-type:  <https://data.norge.no/vocabulary/relationship-with-source-type#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon for allmennheten"@nb ;
                            dct:audience                    audience-type:public ;
                            skosno:relationshipWithSource   relationship-with-source-type:derived-from-source ;
                          ] .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/definisjonForAllmennheten", it.path)

                assertTrue(it.value is Definisjon)
                val xlDefinition = it.value as Definisjon

                assertEquals(ForholdTilKildeEnum.SITATFRAKILDE, xlDefinition.kildebeskrivelse!!.forholdTilKilde)
            }
        }
    }

    @Test
    fun `should extract definisjonForSpesialister`() {
        val turtle = """
            @prefix rdf:                            <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:                           <http://www.w3.org/2004/02/skos/core#> .
            @prefix euvoc:                          <http://publications.europa.eu/ontology/euvoc#> .
            @prefix skosno:                         <https://data.norge.no/vocabulary/skosno#> .
            @prefix dct:                            <http://purl.org/dc/terms/> .
            @prefix audience-type:                  <https://data.norge.no/vocabulary/audience-type#> .
            @prefix relationship-with-source-type:  <https://data.norge.no/vocabulary/relationship-with-source-type#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon for spesialister"@nb ;
                            dct:audience                    audience-type:specialist ;
                            skosno:relationshipWithSource   relationship-with-source-type:direct-from-source ;
                          ] .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/definisjonForSpesialister", it.path)

                assertTrue(it.value is Definisjon)
                val xlDefinition = it.value as Definisjon

                assertEquals(ForholdTilKildeEnum.BASERTPAAKILDE, xlDefinition.kildebeskrivelse!!.forholdTilKilde)
            }
        }
    }

    @Test
    fun `should fail to extract definisjon`() {
        val turtle = """
            @prefix rdf:                            <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:                           <http://www.w3.org/2004/02/skos/core#> .
            @prefix euvoc:                          <http://publications.europa.eu/ontology/euvoc#> .
            @prefix skosno:                         <https://data.norge.no/vocabulary/skosno#> .
            @prefix dct:                            <http://purl.org/dc/terms/> .
            @prefix relationship-with-source-type:  <https://data.norge.no/vocabulary/relationship-with-source-type#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb, "recommendedTerm"@en ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            dct:source                      "kap14", <https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14> ;
                          ] .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.issues.size)

            result.issues.first().let {
                assertEquals(IssueType.WARNING, it.type)
                assertEquals("[euvoc:xlDefinition] Invalid type for skosno:relationshipWithSource", it.message)
            }

            result.issues.last().let {
                assertEquals(IssueType.ERROR, it.type)
                assertEquals("[euvoc:xlDefinition] Missing rdf:value", it.message)
            }
        }
    }

    @Test
    fun `should extract merknad`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    skos:scopeNote        "merknad"@nb, "merknad uten språktag" .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/merknad", it.path)

                assertTrue(it.value is Map<*, *>)
                val scopeNotes = it.value as Map<*, *>

                assertEquals(1, scopeNotes.size)
                assertEquals("merknad", scopeNotes["nb"])

                assertEquals(1, result.issues.size)

                result.issues.first().let {
                    assertEquals(IssueType.WARNING, it.type)
                    assertEquals("Missing language tag for skos:scopeNote: merknad uten språktag", it.message)
                }
            }
        }
    }

    @Test
    fun `should extract eksempel`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    skos:example          "eksempel"@nb, "eksempel uten språktag" .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/eksempel", it.path)

                assertTrue(it.value is Map<*, *>)
                val examples = it.value as Map<*, *>

                assertEquals(1, examples.size)
                assertEquals("eksempel", examples["nb"])
            }

            assertEquals(1, result.issues.size)
            result.issues.first().let {
                assertEquals(IssueType.WARNING, it.type)
                assertEquals("Missing language tag for skos:example: eksempel uten språktag", it.message)
            }
        }
    }

    @Test
    fun `should extract fagområde`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .            
            @prefix dct:   <http://purl.org/dc/terms/> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    dct:subject           "fagområde"@nb, "fagområde uten språktag" .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/fagområde", it.path)

                assertTrue(it.value is Map<*, *>)
                val subjects = (it.value as Map<*, *>)["nb"] as List<*>

                assertEquals(setOf("fagområde"), subjects.toSet())
            }

            assertEquals(1, result.issues.size)
            result.issues.first().let {
                assertEquals(IssueType.WARNING, it.type)
                assertEquals("Missing language tag for dct:subject: fagområde uten språktag", it.message)
            }
        }
    }

    @Test
    fun `should extract omfang`() {
        val turtle = """
            @prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:   <http://www.w3.org/2004/02/skos/core#> .            
            @prefix skosno: <https://data.norge.no/vocabulary/skosno#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    skosno:valueRange     "omfang", <https://example.com/omfang> .

        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/omfang", it.path)

                assertTrue(it.value is URITekst)
                val valueRange = it.value as URITekst

                assertEquals("omfang", valueRange.tekst)
                assertEquals("https://example.com/omfang", valueRange.uri)
            }
        }
    }

    @Test
    fun `should extract gyldigFom`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .            
            @prefix euvoc: <http://publications.europa.eu/ontology/euvoc#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:startDate       "2020-12-31"^^xsd:date .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/gyldigFom", it.path)

                assertTrue(it.value is LocalDate)
                val startDate = it.value as LocalDate

                assertEquals(LocalDate.of(2020, 12, 31), startDate)
            }
        }
    }

    @Test
    fun `should fail to extract gyldigFom`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .            
            @prefix euvoc: <http://publications.europa.eu/ontology/euvoc#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:startDate       "2020"^^xsd:date .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(1, result.issues.size)

            result.issues.first().let {
                assertEquals(IssueType.ERROR, it.type)
                assertEquals("Invalid date for euvoc:startDate: 2020", it.message)
            }
        }
    }

    @Test
    fun `should extract gyldigTom`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .            
            @prefix euvoc: <http://publications.europa.eu/ontology/euvoc#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:endDate         "2025-12-31"^^xsd:date .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/gyldigTom", it.path)

                assertTrue(it.value is LocalDate)
                val endDate = it.value as LocalDate

                assertEquals(LocalDate.of(2025, 12, 31), endDate)
            }
        }
    }

    @Test
    fun `should fail to extract gyldigTom`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .            
            @prefix euvoc: <http://publications.europa.eu/ontology/euvoc#> .

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:endDate         "2025"^^xsd:date .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(1, result.issues.size)

            result.issues.first().let {
                assertEquals(IssueType.ERROR, it.type)
                assertEquals("Invalid date for euvoc:endDate: 2025", it.message)
            }
        }
    }

    @Test
    fun `should extract kontaktpunkt`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix vcard: <http://www.w3.org/2006/vcard/ns#> .
            @prefix dcat:  <http://www.w3.org/ns/dcat#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .            

            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    dcat:contactPoint     
                          [ 
                            rdf:type                vcard:Organization ;
                            vcard:hasEmail          <mailto:organization@example.com> ;
                            vcard:hasTelephone      <tel:+123-ABC-789> 
                          ].
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)

        val extractResultWrappers = model.extract()
        assertEquals(1, extractResultWrappers.size)

        extractResultWrappers.first().extractResult.let { result ->
            assertEquals(2, result.operations.size)

            result.operations.last().let {
                assertEquals(OpEnum.ADD, it.op)
                assertEquals("/kontaktpunkt", it.path)

                assertTrue(it.value is Kontaktpunkt)
                val contactPoint = it.value as Kontaktpunkt

                assertEquals("organization@example.com", contactPoint.harEpost)

                assertEquals(1, result.issues.size)

                result.issues.first().let {
                    assertEquals(IssueType.WARNING, it.type)
                    assertEquals(
                        "[dcat:contactPoint] Invalid telephone for vcard:hasTelephone: +123-ABC-789",
                        it.message
                    )
                }
            }
        }
    }
}

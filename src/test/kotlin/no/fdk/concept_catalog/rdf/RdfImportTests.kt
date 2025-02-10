package no.fdk.concept_catalog.rdf

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.service.createNewConcept
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
class RdfImportTests {
    lateinit var model: Model

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    @BeforeEach
    fun setup() {
        model = ModelFactory.createDefaultModel()
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(SemVer(1, 0, 0), conceptExtraction.concept.versjonsnr)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(3, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.REPLACE && it.path == "/versjonsnr/major" && it.value == 1
            })

            assertTrue(result.operations.any {
                it.op == OpEnum.REPLACE && it.path == "/versjonsnr/minor" && it.value == 0
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.ERROR && it.message.startsWith("versionInfo")
            })
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
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:status          <http://publications.europa.eu/resource/authority/concept-status/CURRENT> .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(
            "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
            conceptExtraction.concept.statusURI
        )

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/statusURI" && it.value == "http://publications.europa.eu/resource/authority/concept-status/CURRENT"
            })
        }
    }

    @Test
    fun `should extract anbefaltTerm`() {
        val turtle = """
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            
            <https://example.com/concept>
                    rdf:type              skos:Concept ;
                    skos:prefLabel        "anbefaltTerm"@nb .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(Term(navn = mapOf("nb" to "anbefaltTerm")), conceptExtraction.concept.anbefaltTerm)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/anbefaltTerm" && it.value == mapOf("navn" to mapOf("nb" to "anbefaltTerm"))
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.ERROR && it.message.startsWith("prefLabel")
            })
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
                    skos:altLabel         "tillattTerm"@nb .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(mapOf("nb" to listOf("tillattTerm")), conceptExtraction.concept.tillattTerm)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/tillattTerm/nb" && it.value == listOf("tillattTerm")
            })
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
                    skos:hiddenLabel      "frarådetTerm"@nb .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(mapOf("nb" to listOf("frarådetTerm")), conceptExtraction.concept.frarådetTerm)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/frarådetTerm/nb" && it.value == listOf("frarådetTerm")
            })
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
                    skos:prefLabel        "anbefaltTerm"@nb ;
                    euvoc:xlDefinition                   
                          [ 
                            rdf:type                        euvoc:XlNote ;
                            rdf:value                       "definisjon"@nb ;
                            skosno:relationshipWithSource   relationship-with-source-type:self-composed ;
                            dct:source                      "kap14", <https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14> ;
                          ] .
        """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(
            Definisjon(
                tekst = mapOf("nb" to "definisjon"),
                kildebeskrivelse = Kildebeskrivelse(
                    forholdTilKilde = ForholdTilKildeEnum.EGENDEFINERT,
                    kilde = listOf(
                        URITekst(uri = "https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14"),
                        URITekst(tekst = "kap14")
                    )
                )
            ), conceptExtraction.concept.definisjon
        )

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/definisjon" && it.value == mapOf(
                    "tekst" to mapOf("nb" to "definisjon"),
                    "kildebeskrivelse" to mapOf(
                        "forholdTilKilde" to "egendefinert",
                        "kilde" to listOf(
                            mapOf(
                                "uri" to "https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14",
                                "tekst" to null
                            ),
                            mapOf(
                                "uri" to null,
                                "tekst" to "kap14"
                            )
                        )
                    )
                )
            })
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
                        skos:prefLabel        "anbefaltTerm"@nb ;
                        euvoc:xlDefinition
                              [
                                rdf:type                        euvoc:XlNote ;
                                rdf:value                       "definisjon for allmennheten"@nb ;
                                dct:audience                    audience-type:public ;
                                skosno:relationshipWithSource   relationship-with-source-type:derived-from-source ;
                              ] .
            """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(
            Definisjon(
                tekst = mapOf("nb" to "definisjon for allmennheten"),
                kildebeskrivelse = Kildebeskrivelse(
                    forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
                )
            ), conceptExtraction.concept.definisjonForAllmennheten
        )

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/definisjonForAllmennheten"
            })
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
                        skos:prefLabel        "anbefaltTerm"@nb ;
                        euvoc:xlDefinition
                              [
                                rdf:type                        euvoc:XlNote ;
                                rdf:value                       "definisjon for spesialister"@nb ;
                                dct:audience                    audience-type:specialist ;
                                skosno:relationshipWithSource   relationship-with-source-type:direct-from-source ;
                              ] .
            """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(
            Definisjon(
                tekst = mapOf("nb" to "definisjon for spesialister"),
                kildebeskrivelse = Kildebeskrivelse(
                    forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
                )
            ), conceptExtraction.concept.definisjonForSpesialister
        )

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/definisjonForSpesialister"
            })
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
                        skos:prefLabel        "anbefaltTerm"@nb ;
                        euvoc:xlDefinition
                              [
                                rdf:type                        euvoc:XlNote ;
                                dct:source                      "kap14", <https://lovdata.no/dokument/NL/lov/1997-02-28-19/kap14#kap14> ;
                                skosno:relationshipWithSource   relationship-with-source-type:direct-from-source ;
                              ] .
            """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.ERROR && it.message.startsWith("xlDefinition")
            })
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
                        skos:scopeNote        "merknad"@nb .
            """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(mapOf("nb" to "merknad"), conceptExtraction.concept.merknad)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/merknad/nb"
            })
        }
    }

    @Test
    fun `should fail to extract merknad`() {
        val turtle = """
                @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .

                <https://example.com/concept>
                        rdf:type              skos:Concept ;
                        skos:prefLabel        "anbefaltTerm"@nb ;
                        skos:scopeNote        "merknad uten språktag" .
            """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.ERROR && it.message.startsWith("scopeNote")
            })
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
                            skos:example          "eksempel"@nb .
                """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(mapOf("nb" to "eksempel"), conceptExtraction.concept.eksempel)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/eksempel/nb"
            })
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
                            dct:subject           "fagområde"@nb .
                """.trimIndent()

        model.read(StringReader(turtle), "https://example.com", Lang.TURTLE.name)
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(mapOf("nb" to listOf("fagområde")), conceptExtraction.concept.fagområde)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/fagområde/nb"
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(URITekst(uri = "https://example.com/omfang", tekst = "omfang"), conceptExtraction.concept.omfang)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/omfang"
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(LocalDate.of(2020, 12, 31), conceptExtraction.concept.gyldigFom)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/gyldigFom"
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.ERROR && it.message.startsWith("startDate")
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(LocalDate.of(2025, 12, 31), conceptExtraction.concept.gyldigTom)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/gyldigTom"
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.ERROR && it.message.startsWith("endDate")
            })
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
        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val resource = model.getResource("https://example.com/concept")
        val conceptExtraction = resource.extract(concept, objectMapper)

        assertEquals(Kontaktpunkt(harEpost = "organization@example.com"), conceptExtraction.concept.kontaktpunkt)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/kontaktpunkt" && it.value == mapOf(
                    "harEpost" to "organization@example.com",
                    "harTelefon" to null
                )
            })

            assertEquals(1, result.issues.size)

            assertTrue(result.issues.any {
                it.type == IssueType.WARNING && it.message.startsWith("contactPoint")
            })
        }
    }
}

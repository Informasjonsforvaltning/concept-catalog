package no.fdk.concept_catalog.rdf

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.concept_catalog.model.*
import no.fdk.concept_catalog.service.createNewConcept
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.time.LocalDate

@Tag("unit")
class RDFImportTests {

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

        assertEquals(
            "http://publications.europa.eu/resource/authority/concept-status/CURRENT",
            conceptExtraction.concept.statusURI
        )

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.REPLACE && it.path == "/statusURI" && it.value == "http://publications.europa.eu/resource/authority/concept-status/CURRENT"
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

        val conceptExtraction = createConceptExtraction(turtle)

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
            @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .
            @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
            @prefix dct:   <http://purl.org/dc/terms/> .
            @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
            @prefix euvoc:  <http://publications.europa.eu/ontology/euvoc#> .
            
            <https://example.com/concept>
                    rdf:type            skos:Concept ;
                    rdfs:seeAlso        <http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322> ;
                    dct:isReplacedBy    <http://begrepskatalogen/begrep/98da4336-dff2-11e7-a0fd-005056821322> ;
                    euvoc:status        <http://publications.europa.eu/resource/authority/concept-status/CURRENT> ;
                    skos:altLabel       "tillattTerm"@nn, "tillattTerm2"@nn ;
                    skos:hiddenLabel    "fraraadetTerm"@nb, "fraraadetTerm2"@nb, "Lorem ipsum"@nb .
        """.trimIndent()

        val conceptExtraction = createConceptExtraction(turtle)

        val jsonPatches = conceptExtraction.extractionRecord.extractResult.operations
        assertEquals (5, jsonPatches.size)

        val allIssues = conceptExtraction.extractionRecord.extractResult.issues
        assertEquals(1, allIssues.size)

        assertTrue ( allIssues.any {
            it.type == IssueType.ERROR && it.message.startsWith("prefLabel")
        }, "Expected an issue with prefLabel extraction error" )
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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

        assertEquals(
            Definisjon(
                tekst = mapOf("nb" to "definisjon for allmennheten"),
                kildebeskrivelse = Kildebeskrivelse(
                    forholdTilKilde = ForholdTilKildeEnum.BASERTPAAKILDE,
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

        val conceptExtraction = createConceptExtraction(turtle)

        assertEquals(
            Definisjon(
                tekst = mapOf("nb" to "definisjon for spesialister"),
                kildebeskrivelse = Kildebeskrivelse(
                    forholdTilKilde = ForholdTilKildeEnum.SITATFRAKILDE,
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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

        val conceptExtraction = createConceptExtraction(turtle)

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

    @Test
    fun `should extract seOgså`() {
        val turtle = """
                        @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                        @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
                        @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .

                        <https://example.com/concept>
                                rdf:type              skos:Concept ;
                                skos:prefLabel        "anbefaltTerm"@nb ;
                                rdfs:seeAlso          <https://example.com/seeAlsoConcept> .
                    """.trimIndent()

        val conceptExtraction = createConceptExtraction(turtle)

        assertEquals(listOf("https://example.com/seeAlsoConcept"), conceptExtraction.concept.seOgså)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/seOgså/0"
            })
        }
    }

    @Test
    fun `should extract erstattesAv`() {
        val turtle = """
                        @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                        @prefix dct:   <http://purl.org/dc/terms/> .
                        @prefix skos:  <http://www.w3.org/2004/02/skos/core#> .

                        <https://example.com/concept>
                                rdf:type              skos:Concept ;
                                skos:prefLabel        "anbefaltTerm"@nb ;
                                dct:isReplacedBy      <https://example.com/isReplacedByConcept> .
                    """.trimIndent()

        val conceptExtraction = createConceptExtraction(turtle)

        assertEquals(listOf("https://example.com/isReplacedByConcept"), conceptExtraction.concept.erstattesAv)

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(2, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/erstattesAv/0"
            })
        }
    }

    @Test
    fun `should extract begrepsRelasjon`() {
        val turtle = """
                        @prefix rdf:    <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                        @prefix dct:    <http://purl.org/dc/terms/> .
                        @prefix skos:   <http://www.w3.org/2004/02/skos/core#> .
                        @prefix skosno: <https://data.norge.no/vocabulary/skosno#> .

                        <https://example.com/concept>
                                rdf:type              skos:Concept ;
                                skos:prefLabel        "anbefaltTerm"@nb ;
                                skosno:isFromConceptIn 
                                      [ 
                                        rdf:type                        skosno:AssociativeConceptRelation ;
                                        skosno:hasToConcept             <https://example.com/topConcept> ; 
                                        skosno:relationRole             "muliggjør"@nb
                                      ] ;
                                skosno:hasPartitiveConceptRelation    
                                      [ 
                                        rdf:type                        skosno:PartitiveConceptRelation ;
                                        dct:description                 "inndelingskriterium"@nb ;
                                        skosno:hasPartitiveConcept      <https://example.com/partitiveConcept>
                                      ] ;
                                skosno:hasPartitiveConceptRelation    
                                      [ 
                                        rdf:type                        skosno:PartitiveConceptRelation ;
                                        dct:description                 "inndelingskriterium"@nb ;
                                        skosno:hasComprehensiveConcept  <https://example.com/comprehensiveConcept>
                                      ] ;
                                skosno:hasGenericConceptRelation      
                                      [ 
                                        rdf:type                        skosno:GenericConceptRelation ;
                                        dct:description                 "inndelingskriterium"@nb ;
                                        skosno:hasGenericConcept        <https://example.com/genericConcept>
                                      ] ;
                                skosno:hasGenericConceptRelation     
                                      [ 
                                        rdf:type                        skosno:GenericConceptRelation ;
                                        dct:description                 "inndelingskriterium"@nb ;
                                        skosno:hasSpecificConcept       <https://example.com/specificConcept>
                                      ] .
                    """.trimIndent()

        val conceptExtraction = createConceptExtraction(turtle)

        conceptExtraction.concept.begrepsRelasjon!!.let { relations ->
            assertEquals(5, relations.size)

            relations.first { it.relasjon == "assosiativ" }
                .let { associative ->
                    assertEquals("https://example.com/topConcept", associative.relatertBegrep)

                    associative.beskrivelse!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "muliggjør")
                    }
                }

            relations.first { it.relasjon == "partitiv" && it.relasjonsType == "omfatter" }
                .let { partitive ->
                    assertEquals("https://example.com/partitiveConcept", partitive.relatertBegrep)

                    partitive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }

            relations.first { it.relasjon == "partitiv" && it.relasjonsType == "erDelAv" }
                .let { comprehensive ->
                    assertEquals("https://example.com/comprehensiveConcept", comprehensive.relatertBegrep)

                    comprehensive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }

            relations.first { it.relasjon == "generisk" && it.relasjonsType == "overordnet" }
                .let { comprehensive ->
                    assertEquals("https://example.com/genericConcept", comprehensive.relatertBegrep)

                    comprehensive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }

            relations.first { it.relasjon == "generisk" && it.relasjonsType == "underordnet" }
                .let { comprehensive ->
                    assertEquals("https://example.com/specificConcept", comprehensive.relatertBegrep)

                    comprehensive.inndelingskriterium!!.let {
                        assertEquals(1, it.size)
                        assertTrue(it.containsKey("nb"))
                        assertEquals(it.getValue("nb"), "inndelingskriterium")
                    }
                }
        }

        conceptExtraction.extractionRecord.extractResult.let { result ->
            assertEquals(6, result.operations.size)

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/begrepsRelasjon/0"
            })

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/begrepsRelasjon/1"
            })

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/begrepsRelasjon/2"
            })

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/begrepsRelasjon/3"
            })

            assertTrue(result.operations.any {
                it.op == OpEnum.ADD && it.path == "/begrepsRelasjon/4"
            })
        }
    }

    private fun createConceptExtraction(turtle: String): ConceptExtraction {
        val model = ModelFactory.createDefaultModel()
        model.read(StringReader(turtle), null, Lang.TURTLE.name)

        val resource = model.getResource("https://example.com/concept")

        val concept = createNewConcept(Virksomhet(id = "id"), user = User(id = "id", name = null, email = null))

        val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

        return resource.extract(concept, objectMapper)
    }
}

package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.utils.BEGREP_0
import no.fdk.concept_catalog.utils.BEGREP_6
import no.fdk.concept_catalog.utils.TestResponseReader
import no.fdk.concept_catalog.utils.toDBO
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("contract")
class GetSkosApNoConcept : ContractTestsBase() {

    @Test
    fun `Get SKOS-AP-NO Concept`() {
        mongoOperations.insert(BEGREP_0.toDBO())

        val expected = TestResponseReader().parseTurtleFile("concept.ttl")

        val turtle =
            request("/collections/123456789/concepts/id0-old", MediaType.valueOf("text/turtle"), HttpMethod.GET)

        assertEquals(HttpStatus.OK, turtle.statusCode)

        val turtleModel =
            ModelFactory.createDefaultModel().read(StringReader(turtle.body as String), null, Lang.TURTLE.name)

        assertTrue(expected.isIsomorphicWith(turtleModel))
    }

    @Test
    fun `Handles Blank URIs in Omfang and Kildebeskrivelse`() {
        mongoOperations.insert(BEGREP_6.toDBO())

        val rdfXml =
            request("/collections/987654321/concepts/id6", MediaType.valueOf("application/rdf+xml"), HttpMethod.GET)
        assertEquals(HttpStatus.OK, rdfXml.statusCode)

        val ldJson =
            request("/collections/987654321/concepts/id6", MediaType.valueOf("application/ld+json"), HttpMethod.GET)
        assertEquals(HttpStatus.OK, ldJson.statusCode)
    }
}

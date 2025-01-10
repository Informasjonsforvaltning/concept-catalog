package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.ContractTestsBase
import no.fdk.concept_catalog.utils.*
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
class GetSkosApNoCollection : ContractTestsBase() {

    @Test
    fun `Get SKOS-AP-NO Collection`() {
        mongoOperations.insertAll(listOf(BEGREP_0.toDBO(), BEGREP_1.toDBO(), BEGREP_2.toDBO()))

        val expected = TestResponseReader().parseTurtleFile("collection_0.ttl")

        val turtle = request("/collections/123456789", MediaType.valueOf("text/turtle"), HttpMethod.GET)
        val n3 = request("/collections/123456789", MediaType.valueOf("text/n3"), HttpMethod.GET)
        val rdfXML = request("/collections/123456789", MediaType.valueOf("application/rdf+xml"), HttpMethod.GET)
        val rdfJSON = request("/collections/123456789", MediaType.valueOf("application/rdf+json"), HttpMethod.GET)
        val ldJSON = request("/collections/123456789", MediaType.valueOf("application/ld+json"), HttpMethod.GET)
        val nTriples = request("/collections/123456789", MediaType.valueOf("application/n-triples"), HttpMethod.GET)
        val nQuads = request("/collections/123456789", MediaType.valueOf("application/n-quads"), HttpMethod.GET)
        val trig = request("/collections/123456789", MediaType.valueOf("application/trig"), HttpMethod.GET)
        val trix = request("/collections/123456789", MediaType.valueOf("application/trix"), HttpMethod.GET)

        assertEquals(HttpStatus.OK, turtle.statusCode)
        assertEquals(HttpStatus.OK, n3.statusCode)
        assertEquals(HttpStatus.OK, rdfXML.statusCode)
        assertEquals(HttpStatus.OK, rdfJSON.statusCode)
        assertEquals(HttpStatus.OK, ldJSON.statusCode)
        assertEquals(HttpStatus.OK, nTriples.statusCode)
        assertEquals(HttpStatus.OK, nQuads.statusCode)
        assertEquals(HttpStatus.OK, trig.statusCode)
        assertEquals(HttpStatus.OK, trix.statusCode)

        val turtleModel =
            ModelFactory.createDefaultModel().read(StringReader(turtle.body as String), null, Lang.TURTLE.name)
        val n3Model = ModelFactory.createDefaultModel().read(StringReader(n3.body as String), null, Lang.N3.name)
        val rdfXMLModel =
            ModelFactory.createDefaultModel().read(StringReader(rdfXML.body as String), null, Lang.RDFXML.name)
        val rdfJSONModel =
            ModelFactory.createDefaultModel().read(StringReader(rdfJSON.body as String), null, Lang.RDFJSON.name)
        val ldJSONModel =
            ModelFactory.createDefaultModel().read(StringReader(ldJSON.body as String), null, Lang.JSONLD.name)
        val nTriplesModel =
            ModelFactory.createDefaultModel().read(StringReader(nTriples.body as String), null, Lang.NTRIPLES.name)
        val nQuadsModel =
            ModelFactory.createDefaultModel().read(StringReader(nQuads.body as String), null, Lang.NQUADS.name)
        val trigModel =
            ModelFactory.createDefaultModel().read(StringReader(trig.body as String), null, Lang.TRIG.name)
        val trixModel =
            ModelFactory.createDefaultModel().read(StringReader(trix.body as String), null, Lang.TRIX.name)

        assertTrue(expected.isIsomorphicWith(turtleModel))
        assertTrue(expected.isIsomorphicWith(n3Model))
        assertTrue(expected.isIsomorphicWith(rdfXMLModel))
        assertTrue(expected.isIsomorphicWith(rdfJSONModel))
        assertTrue(expected.isIsomorphicWith(ldJSONModel))
        assertTrue(expected.isIsomorphicWith(nTriplesModel))
        assertTrue(expected.isIsomorphicWith(nQuadsModel))
        assertTrue(expected.isIsomorphicWith(trigModel))
        assertTrue(expected.isIsomorphicWith(trixModel))
    }
}

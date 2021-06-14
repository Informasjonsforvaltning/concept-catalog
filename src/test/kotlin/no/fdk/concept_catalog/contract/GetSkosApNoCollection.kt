package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.TestResponseReader
import no.fdk.concept_catalog.utils.apiGet
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import java.io.StringReader
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class GetSkosApNoCollection: ApiTestContext() {

    @Test
    fun `Get SKOS-AP-NO Collection`() {
        val expected = TestResponseReader().parseTurtleFile("collection_0.ttl")

        val turtle = apiGet(port, "/collections/123456789", MediaType.valueOf("text/turtle"))
        val n3 = apiGet(port, "/collections/123456789", MediaType.valueOf("text/n3"))
        val rdfXML = apiGet(port, "/collections/123456789", MediaType.valueOf("application/rdf+xml"))
        val rdfJSON = apiGet(port, "/collections/123456789", MediaType.valueOf("application/rdf+json"))
        val ldJSON = apiGet(port, "/collections/123456789", MediaType.valueOf("application/ld+json"))
        val nTriples = apiGet(port, "/collections/123456789", MediaType.valueOf("application/n-triples"))
        val nQuads = apiGet(port, "/collections/123456789", MediaType.valueOf("application/n-quads"))
        val trig = apiGet(port, "/collections/123456789", MediaType.valueOf("application/trig"))
        val trix = apiGet(port, "/collections/123456789", MediaType.valueOf("application/trix"))

        assertTrue { HttpStatus.OK.value() == turtle["status"] }
        assertTrue { HttpStatus.OK.value() == n3["status"] }
        assertTrue { HttpStatus.OK.value() == rdfXML["status"] }
        assertTrue { HttpStatus.OK.value() == rdfJSON["status"] }
        assertTrue { HttpStatus.OK.value() == ldJSON["status"] }
        assertTrue { HttpStatus.OK.value() == nTriples["status"] }
        assertTrue { HttpStatus.OK.value() == nQuads["status"] }
        assertTrue { HttpStatus.OK.value() == trig["status"] }
        assertTrue { HttpStatus.OK.value() == trix["status"] }

        val turtleModel = ModelFactory.createDefaultModel().read(StringReader(turtle["body"] as String), null, Lang.TURTLE.name)
        val n3Model = ModelFactory.createDefaultModel().read(StringReader(n3["body"] as String), null, Lang.N3.name)
        val rdfXMLModel = ModelFactory.createDefaultModel().read(StringReader(rdfXML["body"] as String), null, Lang.RDFXML.name)
        val rdfJSONModel = ModelFactory.createDefaultModel().read(StringReader(rdfJSON["body"] as String), null, Lang.RDFJSON.name)
        val ldJSONModel = ModelFactory.createDefaultModel().read(StringReader(ldJSON["body"] as String), null, Lang.JSONLD.name)
        val nTriplesModel = ModelFactory.createDefaultModel().read(StringReader(nTriples["body"] as String), null, Lang.NTRIPLES.name)
        val nQuadsModel = ModelFactory.createDefaultModel().read(StringReader(nQuads["body"] as String), null, Lang.NQUADS.name)
        val trigModel = ModelFactory.createDefaultModel().read(StringReader(trig["body"] as String), null, Lang.TRIG.name)
        val trixModel = ModelFactory.createDefaultModel().read(StringReader(trix["body"] as String), null, Lang.TRIX.name)

        assertTrue { expected.isIsomorphicWith(turtleModel) }
        assertTrue { expected.isIsomorphicWith(n3Model) }
        assertTrue { expected.isIsomorphicWith(rdfXMLModel) }
        assertTrue { expected.isIsomorphicWith(rdfJSONModel) }
        assertTrue { expected.isIsomorphicWith(ldJSONModel) }
        assertTrue { expected.isIsomorphicWith(nTriplesModel) }
        assertTrue { expected.isIsomorphicWith(nQuadsModel) }
        assertTrue { expected.isIsomorphicWith(trigModel) }
        assertTrue { expected.isIsomorphicWith(trixModel) }
    }

}

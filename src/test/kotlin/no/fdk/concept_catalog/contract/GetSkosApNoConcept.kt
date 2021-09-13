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
class GetSkosApNoConcept: ApiTestContext() {

    @Test
    fun `Get SKOS-AP-NO Concept`() {
        val expected = TestResponseReader().parseTurtleFile("concept.ttl")

        val turtle = apiGet(port, "/collections/123456789/concepts/id0-old", MediaType.valueOf("text/turtle"))

        assertTrue { HttpStatus.OK.value() == turtle["status"] }

        val turtleModel = ModelFactory.createDefaultModel().read(StringReader(turtle["body"] as String), null, Lang.TURTLE.name)

        assertTrue { expected.isIsomorphicWith(turtleModel) }
    }

}

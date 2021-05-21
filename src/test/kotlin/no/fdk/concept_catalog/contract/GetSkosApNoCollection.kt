package no.fdk.concept_catalog.contract

import no.fdk.concept_catalog.utils.ApiTestContext
import no.fdk.concept_catalog.utils.TestResponseReader
import no.fdk.concept_catalog.utils.apiGet
import org.apache.jena.rdf.model.ModelFactory
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

        val response = apiGet(port, "/collections/123456789", MediaType.valueOf("text/turtle"))
        assertTrue { HttpStatus.OK.value() == response["status"] }

        val rspModel = ModelFactory.createDefaultModel().read(StringReader(response["body"] as String), null, "TURTLE")
        assertTrue { expected.isIsomorphicWith(rspModel) }
    }

}

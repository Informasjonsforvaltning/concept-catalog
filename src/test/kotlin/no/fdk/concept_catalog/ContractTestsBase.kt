package no.fdk.concept_catalog

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.model.CurrentConcept
import no.fdk.concept_catalog.model.ImportResult
import no.fdk.concept_catalog.utils.JwkStore
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.DeleteQuery
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ActiveProfiles("contract-test")
@Import(TestcontainersConfig::class, ElasticTestConfig::class)
@EnableWireMock(ConfigureWireMock(port = 6000))
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
open class ContractTestsBase {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    lateinit var mongoOperations: MongoOperations

    @Autowired
    lateinit var elasticsearchOperations: ElasticsearchOperations

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @BeforeEach
    fun setUp() {
        stubFor(get(urlPathEqualTo("/auth/realms/fdk/protocol/openid-connect/certs")).willReturn(okJson(JwkStore.get())))

        mongoOperations.remove(Query(), Begrep::class.java)
        mongoOperations.remove(Query(), ChangeRequest::class.java)
        mongoOperations.remove(Query(), ImportResult::class.java)

        elasticsearchOperations.delete(
            DeleteQuery.builder(org.springframework.data.elasticsearch.core.query.Query.findAll()).build(),
            CurrentConcept::class.java
        )
        elasticsearchOperations.indexOps(CurrentConcept::class.java).refresh()
    }

    fun addToElasticsearchIndex(concept: CurrentConcept) {
        elasticsearchOperations.save(concept)
        elasticsearchOperations.indexOps(CurrentConcept::class.java).refresh()
    }

    fun addToElasticsearchIndex(concepts: List<CurrentConcept>) {
        elasticsearchOperations.save(concepts)
        elasticsearchOperations.indexOps(CurrentConcept::class.java).refresh()
    }

    fun request(path: String, mediaType: MediaType, httpMethod: HttpMethod): ResponseEntity<String> {
        val url = "http://localhost:$port$path"

        val httpHeaders = HttpHeaders()
        httpHeaders.accept = listOf(mediaType)

        val httpEntity: HttpEntity<String> = HttpEntity(httpHeaders)

        return testRestTemplate.exchange(url, httpMethod, httpEntity, String::class.java)
    }

    fun authorizedRequest(
        path: String,
        body: String? = null,
        token: String? = null,
        httpMethod: HttpMethod,
        accept: MediaType = MediaType.APPLICATION_JSON,
        contentType: MediaType = MediaType.APPLICATION_JSON,
    ): ResponseEntity<String> {
        val url = "http://localhost:$port$path"

        val headers = HttpHeaders()
        headers.accept = listOf(accept)
        headers.contentType = contentType

        token?.let { headers.setBearerAuth(it) }

        val httpEntity: HttpEntity<String> = HttpEntity(body, headers)

        return testRestTemplate.exchange(url, httpMethod, httpEntity, String::class.java)
    }
}

package no.fdk.concept_catalog

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.fdk.concept_catalog.model.Begrep
import no.fdk.concept_catalog.model.ChangeRequest
import no.fdk.concept_catalog.utils.jwk.JwkStore
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock

@ActiveProfiles("contract-test")
@Import(TestcontainersConfig::class)
@EnableWireMock(ConfigureWireMock(port = 6000))
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
open class ContractTestsBase {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var operations: MongoOperations

    @Autowired
    lateinit var mapper: ObjectMapper

    @Autowired
    lateinit var testRestTemplate: TestRestTemplate

    @BeforeEach
    fun setUp() {
        stubFor(get(urlPathEqualTo("/auth/realms/fdk/protocol/openid-connect/certs")).willReturn(okJson(JwkStore.get())))

        operations.remove(Query(), Begrep::class.java)
        operations.remove(Query(), ChangeRequest::class.java)
    }

    fun request(path: String, mediaType: MediaType, httpMethod: HttpMethod): ResponseEntity<String> {
        val url = "http://localhost:$port/$path"

        val httpHeaders = HttpHeaders()
        httpHeaders.set(HttpHeaders.ACCEPT, mediaType.toString())

        val httpEntity: HttpEntity<String> = HttpEntity(httpHeaders)

        val response = testRestTemplate.exchange(
            url,
            httpMethod,
            httpEntity,
            String::class.java
        )

        return response
    }
}
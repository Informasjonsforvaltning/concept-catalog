package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.client.RestTemplate
import java.net.URI
import kotlin.test.assertEquals

@Tag("unit")
class ConceptPublisherTest {

    private val applicationProperties = ApplicationProperties(
        collectionBaseUri = "http://collection-base",
        historyServiceUri = "http://history",
        adminServiceUri = "http://catalog-admin",
        harvestAdminUri = "http://harvest-admin",
    )

    private val restTemplate: RestTemplate = mock()
    private val conceptPublisher = ConceptPublisher(applicationProperties, restTemplate)

    private val publisherId = "123456789"

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `triggerHarvest posts start harvesting request with bearer token when present`() {
        val jwt = Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .claim("sub", "user")
            .build()

        val auth = object : AbstractAuthenticationToken(emptyList()) {
            override fun getCredentials(): Any = ""
            override fun getPrincipal(): Any = jwt
        }
        auth.isAuthenticated = true
        SecurityContextHolder.getContext().authentication = auth

        conceptPublisher.triggerHarvest(publisherId)

        val entityCaptor = argumentCaptor<HttpEntity<Any>>()
        verify(restTemplate).postForEntity(
            eq(URI("http://harvest-admin/organizations/$publisherId/datasources/start-harvesting")),
            entityCaptor.capture(),
            eq(Any::class.java)
        )

        val captured = entityCaptor.firstValue as HttpEntity<*>
        val headers = captured.headers
        val body = captured.body as StartHarvestByUrlRequest

        assertEquals(MediaType.APPLICATION_JSON, headers.contentType)
        assertEquals("Bearer token-value", headers.getFirst(HttpHeaders.AUTHORIZATION))
        assertEquals("http://collection-base/collections/$publisherId", body.url)
        assertEquals("concept", body.dataType)
    }

    @Test
    fun `createNewDataSource posts correct payload and headers`() {
        val jwt = Jwt.withTokenValue("token-value")
            .header("alg", "none")
            .claim("sub", "user")
            .build()

        val auth = object : AbstractAuthenticationToken(emptyList()) {
            override fun getCredentials(): Any = ""
            override fun getPrincipal(): Any = jwt
        }
        auth.isAuthenticated = true
        SecurityContextHolder.getContext().authentication = auth

        conceptPublisher.createNewDataSource(publisherId)

        val entityCaptor = argumentCaptor<HttpEntity<Any>>()
        verify(restTemplate).postForEntity(
            eq(URI("http://harvest-admin/organizations/$publisherId/datasources")),
            entityCaptor.capture(),
            eq(Any::class.java)
        )

        val captured = entityCaptor.firstValue as HttpEntity<*>
        val headers = captured.headers
        val body = captured.body as Any

        val bodyString = body.toString()

        assertEquals(MediaType.APPLICATION_JSON, headers.contentType)
        assertEquals("Bearer token-value", headers.getFirst(HttpHeaders.AUTHORIZATION))
        kotlin.test.assertTrue(bodyString.contains("dataSourceType=SKOS-AP-NO"))
        kotlin.test.assertTrue(bodyString.contains("dataType=concept"))
        kotlin.test.assertTrue(bodyString.contains("url=http://collection-base/collections/$publisherId"))
        kotlin.test.assertTrue(bodyString.contains("acceptHeaderValue=text/turtle"))
        kotlin.test.assertTrue(bodyString.contains("publisherId=$publisherId"))
    }
}

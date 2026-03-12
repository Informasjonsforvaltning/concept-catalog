package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.configuration.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

private val logger = LoggerFactory.getLogger(ConceptPublisher::class.java)

@Service
class ConceptPublisher(
    private val applicationProperties: ApplicationProperties,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private fun dataSourceUrl(publisherId: String): String =
        UriComponentsBuilder
            .fromUriString(applicationProperties.collectionBaseUri)
            .replacePath("/collections/$publisherId")
            .build().toUriString()

    fun triggerHarvest(publisherId: String) {
        val url = UriComponentsBuilder
            .fromUriString(applicationProperties.harvestAdminUri)
            .replacePath("/organizations/$publisherId/datasources/start-harvesting")
            .build().toUriString()

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            resolveBearerToken()?.let { set(HttpHeaders.AUTHORIZATION, "Bearer $it") }
        }

        val body = StartHarvestByUrlRequest(
            url = dataSourceUrl(publisherId),
            dataType = "concept",
        )

        runCatching {
            restTemplate.postForEntity<Any>(URI(url), HttpEntity(body, headers))
        }.onFailure {
            logger.error("Error calling Harvest Admin startHarvestingByUrlAndDataType for catalog {}", publisherId, it)
        }
    }

    fun createNewDataSource(publisherId: String) {
        val url = UriComponentsBuilder
            .fromUriString(applicationProperties.harvestAdminUri)
            .replacePath("/organizations/$publisherId/datasources")
            .build().toUriString()

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            resolveBearerToken()?.let { set(HttpHeaders.AUTHORIZATION, "Bearer $it") }
        }

        val body = HarvestAdminDataSource(
            dataSourceType = "SKOS-AP-NO",
            dataType = "concept",
            url = dataSourceUrl(publisherId),
            acceptHeaderValue = "text/turtle",
            publisherId = publisherId,
            description = "Automatically generated data source for $publisherId"
        )

        runCatching {
            restTemplate.postForEntity<Any>(URI(url), HttpEntity(body, headers))
        }.onFailure {
            logger.error("Error calling Harvest Admin createDataSource for catalog {}", publisherId, it)
        }
    }

    private fun resolveBearerToken(): String? =
        (SecurityContextHolder.getContext().authentication?.principal as? Jwt)
            ?.tokenValue
}

private data class HarvestAdminDataSource(
    val dataSourceType: String,
    val dataType: String,
    val url: String,
    val acceptHeaderValue: String? = null,
    val publisherId: String,
    val description: String? = null,
)

data class StartHarvestByUrlRequest(
    val url: String,
    val dataType: String,
)

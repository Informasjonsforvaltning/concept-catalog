package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.concept_catalog.configuration.ApplicationProperties
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.HistoricPayload
import no.fdk.concept_catalog.model.JsonPatchOperation
import no.fdk.concept_catalog.model.User
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private val logger = LoggerFactory.getLogger(HistoryService::class.java)

@Service
class HistoryService(
    private val applicationProperties: ApplicationProperties,
    private val mapper: ObjectMapper
) {
    fun updateHistory(concept: BegrepDBO, operations: List<JsonPatchOperation>, user: User, jwt: Jwt): String? =
        URI("${applicationProperties.historyServiceUri}/${concept.ansvarligVirksomhet.id}/${concept.id}/updates").toURL()
            .let { it.openConnection() as HttpURLConnection }
            .postUpdateToHistoryService(HistoricPayload(user, operations), jwt)

    private fun HttpURLConnection.postUpdateToHistoryService(payload: HistoricPayload, jwt: Jwt): String? {
        setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer ${jwt.tokenValue}")
        setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
        requestMethod = HttpMethod.POST.toString()
        doOutput = true
        outputStream.write(mapper.writeValueAsBytes(payload))

        if (HttpStatus.valueOf(responseCode).is2xxSuccessful) {
            return headerFields[HttpHeaders.LOCATION]?.first()
        } else {
            logger.error("Failed history update for $url, status: $responseCode")
            throw Exception("Failed history update")
        }
    }

    fun removeHistoryUpdate(location: String, jwt: Jwt) {
        URI("${applicationProperties.historyServiceUri}$location").toURL()
            .let { it.openConnection() as HttpURLConnection }
            .deleteFromHistoryService(jwt)
    }

    private fun HttpURLConnection.deleteFromHistoryService(jwt: Jwt) {
        setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer ${jwt.tokenValue}")
        requestMethod = HttpMethod.DELETE.toString()

        if (!HttpStatus.valueOf(responseCode).is2xxSuccessful) {
            logger.error("Failed history deletion for $url, status: $responseCode")
        }
    }

}

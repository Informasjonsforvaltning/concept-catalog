package no.fdk.concept_catalog.utils

import java.io.BufferedReader
import java.net.URL
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.net.HttpURLConnection

fun apiGet(port: Int, endpoint: String, acceptHeader: MediaType): Map<String,Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        connection.setRequestProperty("Accept", acceptHeader.toString())
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

private fun isOK(response: Int?): Boolean =
    if(response == null) false
    else HttpStatus.resolve(response)?.is2xxSuccessful == true

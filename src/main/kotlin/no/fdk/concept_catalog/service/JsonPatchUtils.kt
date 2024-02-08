package no.fdk.concept_catalog.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.json.Json
import jakarta.json.JsonException
import no.fdk.concept_catalog.APP_LOG
import no.fdk.concept_catalog.model.JsonPatchOperation
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.io.StringReader

inline fun <reified T> patchOriginal(original: T, operations: List<JsonPatchOperation>, mapper: ObjectMapper): T {
    try {
        return applyPatch(original, operations, mapper)
    } catch (ex: Exception) {
        APP_LOG.error("failed to apply json patch operations", ex)
        when (ex) {
            is JsonException -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            is JsonProcessingException -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            is IllegalArgumentException -> throw ResponseStatusException(HttpStatus.BAD_REQUEST, ex.message)
            else -> throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.message)
        }
    }
}

inline fun <reified T> applyPatch(originalObject: T, operations: List<JsonPatchOperation>, mapper: ObjectMapper): T {
    if (operations.isNotEmpty()) {
        with(mapper) {
            val changes = Json.createReader(StringReader(writeValueAsString(operations))).readArray()
            val original = Json.createReader(StringReader(writeValueAsString(originalObject))).readObject()

            return Json.createPatch(changes).apply(original)
                .let { readValue(it.toString()) }
        }
    }
    return originalObject
}

inline fun <reified T> createPatchOperations(originalObject: T, updatedObject: T, mapper: ObjectMapper): List<JsonPatchOperation> =
    with(mapper) {
        val original = Json.createReader(StringReader(writeValueAsString(originalObject))).readObject()
        val updated = Json.createReader(StringReader(writeValueAsString(updatedObject))).readObject()

        return readValue(Json.createDiff(original, updated).toString())
    }

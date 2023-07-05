package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "changeRequest")
data class ChangeRequest(
        @Id
        val id: String,
        val conceptId: String?,
        val catalogId: String,
        val status: ChangeRequestStatus,
        val anbefaltTerm: Term?,
        val tillattTerm: Map<String, List<String>>?,
        val frarådetTerm: Map<String, List<String>>?,
        val definisjon: Definisjon?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChangeRequestForCreate(
    val conceptId: String?,
    val anbefaltTerm: Term?,
    val tillattTerm: Map<String, List<String>>?,
    val frarådetTerm: Map<String, List<String>>?,
    val definisjon: Definisjon?
)

enum class ChangeRequestStatus {
    OPEN, REJECTED, ACCEPTED
}

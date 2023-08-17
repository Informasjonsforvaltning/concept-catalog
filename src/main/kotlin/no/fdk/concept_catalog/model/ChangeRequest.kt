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
    val operations: List<JsonPatchOperation>
)

enum class ChangeRequestStatus {
    OPEN, REJECTED, ACCEPTED
}

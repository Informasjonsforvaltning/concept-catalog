package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "changeRequest")
data class ChangeRequest(
    @Id
    val id: String,
    val conceptId: String?,
    val catalogId: String,
    val status: ChangeRequestStatus,
    val operations: List<JsonPatchOperation>,
    val proposedBy: User,
    val timeForProposal: Instant,
    val title: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChangeRequestUpdateBody(
     val conceptId: String?,
    val operations: List<JsonPatchOperation>,
    val title: String
)

enum class ChangeRequestStatus {
    OPEN, REJECTED, ACCEPTED
}

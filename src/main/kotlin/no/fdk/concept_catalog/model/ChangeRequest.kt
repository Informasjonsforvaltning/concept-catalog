package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "change_requests")
data class ChangeRequest(
    @Id
    @Column(name = "id")
    val id: String = "",

    @Column(name = "concept_id")
    val conceptId: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concept_snapshot", columnDefinition = "jsonb")
    val conceptSnapshot: Begrep? = null,

    @Column(name = "catalog_id", nullable = false)
    val catalogId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ChangeRequestStatus = ChangeRequestStatus.OPEN,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operations", nullable = false, columnDefinition = "jsonb")
    val operations: List<JsonPatchOperation> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_by", nullable = false, columnDefinition = "jsonb")
    val proposedBy: User = User(id = "", name = "", email = null),

    @Column(name = "time_for_proposal", nullable = false)
    val timeForProposal: Instant = Instant.now(),

    @Column(name = "title", nullable = false)
    val title: String = ""
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

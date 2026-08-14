package no.fdk.concept_catalog.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "change_requests")
data class ChangeRequest(
    @Id
    @Column(name = "id")
    val id: String,
    @Column(name = "concept_id")
    val conceptId: String?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "concept_snapshot", columnDefinition = "jsonb")
    val conceptSnapshot: Begrep?,
    @Column(name = "catalog_id", nullable = false)
    val catalogId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ChangeRequestStatus,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "operations", nullable = false, columnDefinition = "jsonb")
    val operations: List<JsonPatchOperation>,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "proposed_by", nullable = false, columnDefinition = "jsonb")
    val proposedBy: User,
    @Column(name = "time_for_proposal", nullable = false)
    val timeForProposal: Instant,
    @Column(name = "title", nullable = false)
    val title: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ChangeRequestUpdateBody(val conceptId: String?, val operations: List<JsonPatchOperation>, val title: String)

enum class ChangeRequestStatus {
    OPEN,
    REJECTED,
    ACCEPTED,
}

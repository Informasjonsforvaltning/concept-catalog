package no.fdk.concept_catalog.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "concepts")
data class ConceptEntity(
    @Id
    @Column(name = "id")
    val id: String,
    @Column(name = "originalt_begrep", nullable = false)
    val originaltBegrep: String,
    @Column(name = "ansvarlig_virksomhet_id", nullable = false)
    val ansvarligVirksomhetId: String,
    @Column(name = "status")
    val status: String?,
    @Column(name = "er_publisert")
    val erPublisert: Boolean?,
    @Column(name = "is_archived")
    val isArchived: Boolean?,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    val data: BegrepDBO,
)

fun BegrepDBO.toEntity(): ConceptEntity =
    ConceptEntity(
        id = id,
        originaltBegrep = originaltBegrep,
        ansvarligVirksomhetId = ansvarligVirksomhet.id,
        status = status?.value,
        erPublisert = erPublisert,
        isArchived = isArchived,
        data = this,
    )

fun ConceptEntity.toDBO(): BegrepDBO = data

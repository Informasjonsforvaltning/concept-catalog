package no.fdk.concept_catalog.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "concepts")
data class ConceptEntity(
    @Id
    @Column(name = "id")
    val id: String = "",

    @Column(name = "originalt_begrep", nullable = false)
    val originaltBegrep: String = "",

    @Column(name = "ansvarlig_virksomhet_id", nullable = false)
    val ansvarligVirksomhetId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    val status: Status? = null,

    @Column(name = "er_publisert")
    val erPublisert: Boolean? = false,

    @Column(name = "is_archived")
    val isArchived: Boolean? = false,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", nullable = false, columnDefinition = "jsonb")
    val data: BegrepDBO
)

fun BegrepDBO.toEntity(): ConceptEntity =
    ConceptEntity(
        id = id,
        originaltBegrep = originaltBegrep,
        ansvarligVirksomhetId = ansvarligVirksomhet.id,
        status = status,
        erPublisert = erPublisert,
        isArchived = isArchived,
        data = this
    )

fun ConceptEntity.toDBO(): BegrepDBO = data

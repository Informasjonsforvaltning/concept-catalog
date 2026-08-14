package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.ConceptEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ConceptRepository : JpaRepository<ConceptEntity, String> {
    fun countByAnsvarligVirksomhetId(orgNr: String): Long

    fun findByAnsvarligVirksomhetId(orgNr: String): List<ConceptEntity>

    fun findByAnsvarligVirksomhetIdAndStatus(orgNr: String, status: String): List<ConceptEntity>

    fun findByOriginaltBegrep(originaltBegrep: String): List<ConceptEntity>

    fun findByOriginaltBegrepAndIsArchived(originaltBegrep: String, isArchived: Boolean): List<ConceptEntity>

    @Query("SELECT DISTINCT c.ansvarligVirksomhetId FROM ConceptEntity c")
    fun findDistinctAnsvarligVirksomhetIds(): List<String>
}

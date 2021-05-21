package no.fdk.concept_catalog.repository

import no.fdk.concept_catalog.model.Begrep
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository


@Repository
interface ConceptRepository : MongoRepository<Begrep, String?>

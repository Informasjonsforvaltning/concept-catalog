package no.fdk.concept_catalog.elastic

import no.fdk.concept_catalog.model.CurrentConcept
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface CurrentConceptRepository : ElasticsearchRepository<CurrentConcept, String>

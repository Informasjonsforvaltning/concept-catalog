package no.fdk.concept_catalog.elastic

import no.fdk.concept_catalog.model.BegrepDBO
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface ConceptSearchRepository : ElasticsearchRepository<BegrepDBO, String>

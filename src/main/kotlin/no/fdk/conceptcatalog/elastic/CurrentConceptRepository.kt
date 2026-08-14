package no.fdk.conceptcatalog.elastic

import no.fdk.conceptcatalog.model.CurrentConcept
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface CurrentConceptRepository : ElasticsearchRepository<CurrentConcept, String>

package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.elastic.ConceptSearchRepository
import no.fdk.concept_catalog.elastic.CurrentConceptRepository
import no.fdk.concept_catalog.elastic.shouldBeCurrent
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.CurrentConcept
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ResetElastic {
    @Autowired
    private lateinit var conceptRepository: MongoTemplate

    @Autowired
    private lateinit var conceptSearchRepository: ConceptSearchRepository

    @Autowired
    private lateinit var currentConceptRepository: CurrentConceptRepository

    fun elasticReindex() {
        val concepts = conceptRepository.findAll<BegrepDBO>()
        conceptSearchRepository.deleteAll()
        currentConceptRepository.deleteAll()
        conceptSearchRepository.saveAll(concepts)
        concepts.forEach {
            if (it.shouldBeCurrent(currentConceptRepository.findByIdOrNull(it.originaltBegrep))) {
                currentConceptRepository.save(CurrentConcept(it))
            }
        }
    }
}

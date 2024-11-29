package no.fdk.concept_catalog.utils

import no.fdk.concept_catalog.elastic.CurrentConceptRepository
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.CurrentConcept
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.stereotype.Component

@Component
class ResetElastic {
    @Autowired
    private lateinit var conceptRepository: MongoTemplate

    @Autowired
    private lateinit var currentConceptRepository: CurrentConceptRepository

    fun elasticReindex() {
        val concepts = conceptRepository.findAll<BegrepDBO>()
        currentConceptRepository.deleteAll()
        concepts
            .groupBy { concept -> concept.originaltBegrep }
            .mapNotNull { pair -> pair.value.maxByOrNull { concept -> concept.versjonsnr } }
            .forEach { currentConceptRepository.save(CurrentConcept(it)) }
    }
}

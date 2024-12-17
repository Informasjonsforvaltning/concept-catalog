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
        currentConceptRepository.deleteAll()

        val groupedByOriginalId = conceptRepository.findAll<BegrepDBO>()
            .groupBy { concept -> concept.originaltBegrep }

        val idsOfHighestPublishedVersion: Map<String, String?> = groupedByOriginalId.mapValues {
            it.value
                .filter { concept -> concept.erPublisert }
                .maxByOrNull { concept -> concept.versjonsnr }
                ?.id
        }

        groupedByOriginalId
            .mapNotNull { pair -> pair.value.maxByOrNull { concept -> concept.versjonsnr } }
            .forEach { currentConceptRepository.save(CurrentConcept(it, idsOfHighestPublishedVersion[it.originaltBegrep])) }
    }
}

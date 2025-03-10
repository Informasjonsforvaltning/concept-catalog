package no.fdk.concept_catalog.elastic

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.CurrentConcept
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(ElasticUpdater::class.java)

@Service
class ElasticUpdater(
    private val conceptRepository: MongoTemplate,
    private val currentConceptRepository: CurrentConceptRepository
) {

    fun reindexElastic() = runBlocking {
        launch {
            try {
                logger.debug("deleting all current concepts")
                currentConceptRepository.deleteAll()
            } catch (_: Exception) { }

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
                .forEach {
                    logger.debug("reindexing ${it.id}, ${it.ansvarligVirksomhet.id}")
                    currentConceptRepository.save(CurrentConcept(it, idsOfHighestPublishedVersion[it.originaltBegrep]))
                }

            logger.info("finished reindexing elastic")
        }
    }

}

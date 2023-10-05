package no.fdk.concept_catalog.elastic

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.fdk.concept_catalog.model.BegrepDBO
import no.fdk.concept_catalog.model.CurrentConcept
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(ElasticUpdater::class.java)

@Service
class ElasticUpdater(
    private val conceptRepository: MongoTemplate,
    private val conceptSearchRepository: ConceptSearchRepository,
    private val currentConceptRepository: CurrentConceptRepository
) {

    fun reindexElastic() = runBlocking {
        launch {
            try {
                conceptSearchRepository.deleteAll()
                currentConceptRepository.deleteAll()
            } catch (_: Exception) { }

            conceptRepository.findAll<BegrepDBO>()
                .forEach {
                    conceptSearchRepository.save(it)
                    if (it.shouldBeCurrent(currentConceptRepository.findByIdOrNull(it.originaltBegrep))) currentConceptRepository.save(CurrentConcept(it))
                }

            logger.info("finished reindexing elastic")
        }
    }

}

fun BegrepDBO.shouldBeCurrent(current: CurrentConcept?): Boolean =
    when {
        current == null -> true
        erPublisert && !current.erPublisert -> true
        !erPublisert && current.erPublisert -> false
        else -> versjonsnr > current.versjonsnr
    }

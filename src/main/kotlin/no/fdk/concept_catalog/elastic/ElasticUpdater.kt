package no.fdk.concept_catalog.elastic

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.fdk.concept_catalog.model.BegrepDBO
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findAll
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(ElasticUpdater::class.java)

@Service
class ElasticUpdater(
    private val conceptRepository: MongoTemplate,
    private val conceptSearchRepository: ConceptSearchRepository
) {

    fun reindexElastic() = runBlocking {
        launch {
            try { conceptSearchRepository.deleteAll() }
            catch (_: Exception) { }

            conceptRepository.findAll<BegrepDBO>()
                .forEach { conceptSearchRepository.save(it) }

            logger.info("finished reindexing elastic")
        }
    }

}

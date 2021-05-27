package no.fdk.concept_catalog.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import no.fdk.concept_catalog.model.DataSource
import no.fdk.concept_catalog.model.DataSourceType
import no.fdk.concept_catalog.model.DataType
import org.springframework.amqp.core.AmqpTemplate
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*

private val logger = LoggerFactory.getLogger(ConceptPublisher::class.java)

private val publishersQueue: Queue<String> = LinkedList()

@Service
class ConceptPublisher(private val rabbitTemplate: AmqpTemplate) {

    @Scheduled(fixedRate = 30000)
    private fun pollQueue() {
        if (publishersQueue.isNotEmpty()) {
            sendHarvestMessage(publishersQueue.poll())
        }
    }

    fun send(publisherId: String) {
        if (!publishersQueue.contains(publisherId)) {
            publishersQueue.add(publisherId)
        }
    }

    private fun sendHarvestMessage(publisherId: String) {
        val payload = JsonNodeFactory.instance.objectNode()
        payload.put("publisherId", publisherId)
        try {
            rabbitTemplate.convertAndSend("harvests", "concept.publisher.HarvestTrigger", payload)
            logger.info("Successfully sent harvest message for publisher {}", publisherId)
        } catch (e: AmqpException) {
            logger.error("Failed to send harvest message for publisher {}", publisherId, e)
        }
    }

    fun sendNewDataSource(publisherId: String, harvestUrl: String) {
        val dataSource = DataSource(
            publisherId = publisherId,
            dataType = DataType.CONCEPT,
            dataSourceType = DataSourceType.SKOS_AP_NO,
            acceptHeaderValue = "text/turtle",
            description = "Automatically generated data source for $publisherId",
            url = harvestUrl
        )
        try {
            rabbitTemplate.convertAndSend("harvests", "concept.publisher.NewDataSource", dataSource)
            logger.info("Successfully sent new datasource message for publisher {}", publisherId)
        } catch (e: AmqpException) {
            logger.error("Failed to send new datasource message for publisher {}", publisherId, e)
        }
    }
}

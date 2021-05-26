package no.fdk.concept_catalog.service

import no.fdk.concept_catalog.model.DataSource
import no.fdk.concept_catalog.model.DataSourceType
import no.fdk.concept_catalog.model.DataType
import org.springframework.amqp.core.AmqpTemplate
import org.slf4j.LoggerFactory
import org.springframework.amqp.AmqpException
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(ConceptPublisher::class.java)

@Service
class ConceptPublisher(private val rabbitTemplate: AmqpTemplate) {

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

package no.fdk.concept_catalog

import co.elastic.clients.transport.TransportOptions
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.testcontainers.elasticsearch.ElasticsearchContainer

@TestConfiguration(proxyBeanMethods = false)
class ElasticTestConfig(private val elasticsearchContainer: ElasticsearchContainer): ElasticsearchConfiguration() {

    @Bean(name = ["elasticsearchTestClientConfiguration"])
    @Primary
    override fun clientConfiguration(): ClientConfiguration {
        val builder = ClientConfiguration.builder()
            .connectedTo(elasticsearchContainer.httpHostAddress)

        return builder.build()
    }

    override fun transportOptions(): TransportOptions {
        val requestOptions = RequestOptions.DEFAULT
            .toBuilder()
            .addHeader("Accept", "application/vnd.elasticsearch+json;compatible-with=8")
            .addHeader("Content-Type", "application/vnd.elasticsearch+json;compatible-with=8")
            .build()

        return Rest5ClientOptions(requestOptions, false)
    }

}

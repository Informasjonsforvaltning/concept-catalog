package no.fdk.concept_catalog

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
        return ClientConfiguration.builder()
            .connectedTo(elasticsearchContainer.httpHostAddress)
            .build()
    }

}

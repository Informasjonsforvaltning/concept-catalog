package no.fdk.concept_catalog.elastic

import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.ssl.SSLContextBuilder
import org.apache.http.ssl.SSLContexts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories
import java.io.File
import javax.net.ssl.SSLContext

@Configuration
@EnableElasticsearchRepositories(basePackages = ["no.fdk.concept_catalog.elastic"])
class ElasticsearchConfig(private val elasticProperties: ElasticProperties): ElasticsearchConfiguration() {

    private fun sslContext(): SSLContext {
        val builder: SSLContextBuilder = SSLContexts.custom()

        builder.loadTrustMaterial(
            File(elasticProperties.storePath),
            elasticProperties.storePass.toCharArray(),
            TrustSelfSignedStrategy()
        )

        return builder.build()
    }

    @Bean(name = ["elasticsearchClientConfiguration"])
    override fun clientConfiguration(): ClientConfiguration {
        val builder = ClientConfiguration.builder()
            .connectedTo(elasticProperties.host)

        if (elasticProperties.ssl) {
            builder.usingSsl(sslContext())
        }

        builder.withBasicAuth(
            elasticProperties.username,
            elasticProperties.password
        )

        return builder.build()
    }

}

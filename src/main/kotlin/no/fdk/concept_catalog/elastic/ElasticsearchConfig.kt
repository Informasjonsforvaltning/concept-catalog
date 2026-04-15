package no.fdk.concept_catalog.elastic

import co.elastic.clients.transport.TransportOptions
import co.elastic.clients.transport.rest5_client.Rest5ClientOptions
import co.elastic.clients.transport.rest5_client.low_level.RequestOptions
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.ssl.SSLContexts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import java.io.File
import javax.net.ssl.SSLContext

@Configuration
open class ElasticsearchConfig(private val elasticProperties: ElasticProperties): ElasticsearchConfiguration() {

    private fun sslContext(): SSLContext {
        val builder: SSLContextBuilder = SSLContexts.custom()

        builder.loadTrustMaterial(
            File(elasticProperties.storePath),
            elasticProperties.storePass.toCharArray()
        )

        return builder.build()
    }

    @Bean(name = ["elasticsearchClientConfiguration"])
    override fun clientConfiguration(): ClientConfiguration {
        val builder = ClientConfiguration.builder()
            .connectedTo(elasticProperties.host)

        if (elasticProperties.ssl) builder.usingSsl(sslContext())

        builder.withBasicAuth(
            elasticProperties.username,
            elasticProperties.password
        )

        return builder.build()
    }

    override fun transportOptions(): TransportOptions {
        val acceptRequestOptions = RequestOptions.DEFAULT
            .toBuilder()
            .addHeader("Accept", "application/vnd.elasticsearch+json;compatible-with=8")
            .addHeader("Content-Type", "application/vnd.elasticsearch+json;compatible-with=8")
            .build()

        return Rest5ClientOptions(acceptRequestOptions, false)
    }

}

package no.fdk.concept_catalog.elastic

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

@Configuration
open class ElasticsearchConfig(private val elasticProperties: ElasticProperties): ElasticsearchConfiguration() {

    private fun sslContext(): SSLContext {
        val trustStore = KeyStore.getInstance(KeyStore.getDefaultType())
        FileInputStream(File(elasticProperties.storePath)).use { inputStream ->
            trustStore.load(inputStream, elasticProperties.storePass.toCharArray())
        }

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(trustStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustManagerFactory.trustManagers, null)
        
        return sslContext
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
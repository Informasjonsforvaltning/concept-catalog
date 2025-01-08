package no.fdk.concept_catalog

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer

@TestConfiguration(proxyBeanMethods = false)
open class TestcontainersConfig {

    @Bean
    @ServiceConnection
    open fun mongoDBContainer(): MongoDBContainer {
        return MongoDBContainer("mongo:latest")
    }

    @Bean
    @ServiceConnection
    open fun elasticsearchContainer(): ElasticsearchContainer {
        return ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.2")
            .withEnv("xpack.security.enabled", "false")
    }
}

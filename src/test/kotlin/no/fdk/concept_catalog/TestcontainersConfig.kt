package no.fdk.concept_catalog

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.elasticsearch.ElasticsearchContainer

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfig {
    @Bean
    @ServiceConnection
    fun postgresContainer(): KPostgreSQLContainer = KPostgreSQLContainer("postgres:16")
        .withDatabaseName("concept_catalog")
        .withUsername("testuser")
        .withPassword("testpassword")

    @Bean
    fun elasticsearchContainer(): ElasticsearchContainer = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.2")
        .withEnv(mapOf(Pair("xpack.security.enabled", "false"), Pair("ES_JAVA_OPTS", "-Xms512M -Xmx512M")))
}

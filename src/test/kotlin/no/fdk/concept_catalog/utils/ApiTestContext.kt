package no.fdk.concept_catalog.utils

import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.mongodb.core.MongoTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.elasticsearch.ElasticsearchContainer
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


abstract class ApiTestContext {

    @LocalServerPort
    var port: Int = 0
    @Autowired
    private lateinit var resetElastic: ResetElastic
    @BeforeEach
    fun resetDatabase() {
        resetDB()
        resetElastic.elasticReindex()
    }

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of(
                "spring.data.mongodb.uri=mongodb://$MONGO_USER:$MONGO_PASSWORD@localhost:${mongoContainer.getMappedPort(MONGO_PORT)}/$MONGO_DB_NAME?authSource=admin&authMechanism=SCRAM-SHA-1",
                "spring.elasticsearch.uris=http://localhost:${elasticContainer.getMappedPort(9200)}"
            ).applyTo(configurableApplicationContext.environment)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ApiTestContext::class.java)
        var mongoContainer: KGenericContainer
        var elasticContainer: ElasticsearchContainer

        init {

            startMockServer()

            mongoContainer = KGenericContainer("mongo:latest")
                .withEnv(MONGO_ENV_VALUES)
                .withExposedPorts(MONGO_PORT)
                .waitingFor(Wait.forListeningPort())

            mongoContainer.start()

            elasticContainer = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.10.2")
                .withEnv(ELASTIC_ENV_VALUES)

            elasticContainer.start()

            resetDB()

            try {
                val con = URL("http://localhost:6000/ping").openConnection() as HttpURLConnection
                con.connect()
                if (con.responseCode != 200) {
                    logger.debug("Ping to mock server failed")
                    stopMockServer()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }

}

// Hack needed because testcontainers use of generics confuses Kotlin
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

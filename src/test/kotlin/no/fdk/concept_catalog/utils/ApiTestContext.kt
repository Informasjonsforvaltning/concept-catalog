package no.fdk.concept_catalog.utils

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext


abstract class ApiTestContext {

    @LocalServerPort
    var port: Int = 0

    internal class Initializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
            TestPropertyValues.of().applyTo(configurableApplicationContext.environment)
        }
    }

}

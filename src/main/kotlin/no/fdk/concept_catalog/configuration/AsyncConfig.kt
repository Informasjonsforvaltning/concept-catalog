package no.fdk.concept_catalog.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@EnableAsync
@Configuration
class AsyncConfig {
    @Bean("import-executor")
    fun importExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = 8
        maxPoolSize = 64
        queueCapacity = 200
        setThreadNamePrefix("import-")
        initialize()
    }
}
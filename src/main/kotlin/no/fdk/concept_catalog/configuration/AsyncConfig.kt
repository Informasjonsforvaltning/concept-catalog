package no.fdk.concept_catalog.configuration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@EnableAsync
@Configuration
class AsyncConfig {
    private val logger: Logger = LoggerFactory.getLogger(AsyncConfig::class.java)

    @Bean("import-executor")
    fun importExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = 8
        maxPoolSize = 64
        queueCapacity = 200
        setThreadNamePrefix("import-")
        setRejectedExecutionHandler { r: Runnable , e: ThreadPoolExecutor ->
            logger.warn(
                "Task rejected due to thread pool \"import-executor\" saturation. Active: {}, Pool size: {}, Queue size: {}",
                e.getActiveCount(), e.getPoolSize(), e.getQueue().size
            )
        }
        initialize()
    }

    @Bean("cancel-import-executor")
    fun cancelExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = 8
        maxPoolSize = 64
        queueCapacity = 200
        setThreadNamePrefix("cancel-import-")
        setRejectedExecutionHandler { r: Runnable , e: ThreadPoolExecutor ->
            logger.warn(
                "Task rejected due to thread pool \"cancel-import-executor\" saturation. Active: {}, Pool size: {}, Queue size: {}",
                e.getActiveCount(), e.getPoolSize(), e.getQueue().size
            )
        }
        initialize()
    }
}
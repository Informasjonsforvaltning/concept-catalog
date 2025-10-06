package no.fdk.concept_catalog.test_config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.task.SyncTaskExecutor
import java.util.concurrent.Executor

@TestConfiguration(proxyBeanMethods = false)
@Profile("contract-test")
class SyncConfig {

    @Bean(name = ["import-executor"])
    @Primary
    fun importExecutor(): Executor = SyncTaskExecutor()

    @Bean(name = ["cancel-import-executor"])
    @Primary
    fun cancelImportExecutor(): Executor = SyncTaskExecutor()

}
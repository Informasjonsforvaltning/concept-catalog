package no.fdk.concept_catalog.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.task.SyncTaskExecutor
import java.util.concurrent.Executor

@Configuration
@Profile("contract-test")
class SyncConfig {

    @Bean(name = ["import-executor"])
    fun importExecutor(): Executor = SyncTaskExecutor()

    @Bean(name = ["cancel-import-executor"])
    fun cancelImportExecutor(): Executor = SyncTaskExecutor()

}
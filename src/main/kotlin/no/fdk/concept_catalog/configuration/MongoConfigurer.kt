package no.fdk.concept_catalog.configuration

import no.fdk.concept_catalog.model.BegrepDBO
import org.bson.Document
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition

@Configuration
class MongoConfigurer {

    @Bean
    fun transactionManager(mongoDatabaseFactory: MongoDatabaseFactory): MongoTransactionManager {
        return MongoTransactionManager(mongoDatabaseFactory)
    }

    @Bean
    fun configureIndexes(mongoOperations: MongoOperations): Boolean {
        val idxOperations = mongoOperations.indexOps(BegrepDBO::class.java)

        idxOperations.ensureIndex(
            CompoundIndexDefinition(Document().append("ansvarligVirksomhet.id", 1))
                .named("ansvarlig_virksomhet")
        )

        idxOperations.ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("ansvarligVirksomhet.id", 1)
                    .append("status", 1)
            )
                .named("ansvarlig_virksomhet_status")
        )

        idxOperations.ensureIndex(
            CompoundIndexDefinition(Document().append("originaltBegrep", 1))
                .named("originalt_begrep")
        )

        idxOperations.ensureIndex(
            CompoundIndexDefinition(
                Document()
                    .append("originaltBegrep", 1)
                    .append("erPublisert", 1)
            )
                .named("originalt_begrep_er_publisert")
        )

        return true
    }

}
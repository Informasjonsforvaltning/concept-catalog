package no.fdk.concept_catalog.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary


@Configuration
class JacksonConfigurer {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        return jacksonObjectMapper {
            configure(KotlinFeature.NullIsSameAsDefault, true)
        }
            .registerModule(JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}

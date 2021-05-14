package no.fdk.concept_catalog.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("security")
data class SecurityProperties(
    val fdkIssuer: String
)

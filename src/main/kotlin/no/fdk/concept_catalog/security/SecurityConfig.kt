package no.fdk.concept_catalog.security

import org.apache.jena.riot.Lang
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.RequestMatcher
import javax.servlet.http.HttpServletRequest

@EnableWebSecurity
open class SecurityConfig {

    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf().disable()
            .cors().and()
            .authorizeRequests{ authorize ->
                authorize.requestMatchers(RDFMatcher()).permitAll()
                    .antMatchers(HttpMethod.OPTIONS).permitAll()
                    .antMatchers(HttpMethod.GET,"/ping").permitAll()
                    .antMatchers(HttpMethod.GET,"/ready").permitAll()
                    .antMatchers(HttpMethod.GET,"/actuator/**").permitAll()
                    .anyRequest().authenticated() }
            .oauth2ResourceServer { resourceServer -> resourceServer.jwt() }
        return http.build()
    }

    @Bean
    open fun jwtDecoder(properties: OAuth2ResourceServerProperties): JwtDecoder? {
        val jwtDecoder = NimbusJwtDecoder.withJwkSetUri(properties.jwt.jwkSetUri).build()
        jwtDecoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                listOf(
                    JwtTimestampValidator(),
                    JwtIssuerValidator(properties.jwt.issuerUri),
                    JwtClaimValidator("aud") { aud: List<String> -> aud.contains("concept-catalogue") }
                )
            ))
        return jwtDecoder
    }
}

private class RDFMatcher: RequestMatcher{
    override fun matches(request: HttpServletRequest?): Boolean =
        request?.method == "GET" && acceptHeaderIsRDF(request.getHeader("Accept"))
}

private fun acceptHeaderIsRDF(accept: String?): Boolean =
    when {
        accept == null -> false
        accept.contains(Lang.TURTLE.headerString) -> true
        accept.contains("text/n3") -> true
        accept.contains(Lang.RDFJSON.headerString) -> true
        accept.contains(Lang.JSONLD.headerString) -> true
        accept.contains(Lang.RDFXML.headerString) -> true
        accept.contains(Lang.NTRIPLES.headerString) -> true
        accept.contains(Lang.NQUADS.headerString) -> true
        accept.contains(Lang.TRIG.headerString) -> true
        accept.contains(Lang.TRIX.headerString) -> true
        else -> false
    }

package no.fdk.concept_catalog.security

import org.apache.jena.riot.Lang
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.web.util.matcher.RequestMatcher
import javax.servlet.http.HttpServletRequest

@Configuration
open class SecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()

        http.cors()
            .and()
                .authorizeRequests()
                .requestMatchers(RDFMatcher())
                    .permitAll()
                .antMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                .antMatchers(HttpMethod.GET,"/ping")
                    .permitAll()
                .antMatchers(HttpMethod.GET,"/ready")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            .and()
                .oauth2ResourceServer()
                    .jwt()
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
        accept.contains(Lang.N3.headerString) -> true
        accept.contains(Lang.RDFJSON.headerString) -> true
        accept.contains(Lang.JSONLD.headerString) -> true
        accept.contains(Lang.RDFXML.headerString) -> true
        accept.contains(Lang.NTRIPLES.headerString) -> true
        accept.contains(Lang.NQUADS.headerString) -> true
        accept.contains(Lang.TRIG.headerString) -> true
        accept.contains(Lang.TRIX.headerString) -> true
        else -> false
    }

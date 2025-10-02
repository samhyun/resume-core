package com.resume.core.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity(useAuthorizationManager = true)
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        // ⬇️ 파라미터 타입을 Mono<AbstractAuthenticationToken> 로 맞춤
        jwtAuthConverter: Converter<Jwt, Mono<AbstractAuthenticationToken>>,
        jwtDecoder: ReactiveJwtDecoder
    ): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange {
                it.pathMatchers("/public/**").permitAll()
                it.anyExchange().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder)
                    jwt.jwtAuthenticationConverter(jwtAuthConverter) // ⬅️ 그대로 주입
                }
            }
            .build()

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder =
        ReactiveJwtDecoders.fromIssuerLocation("http://localhost:8080/realms/test-realm")

    /**
     * realm_access.roles / resource_access.{client}.roles -> ROLE_* 매핑
     */
    @Bean
    fun jwtAuthConverter(): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        val delegate = JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
                val authorities = mutableSetOf<GrantedAuthority>()

                val realmAccess = jwt.getClaim<Map<String, Any>?>("realm_access")
                val realmRoles = (realmAccess?.get("roles") as? Collection<*>) ?: emptyList<Any>()
                realmRoles.forEach { r -> authorities += SimpleGrantedAuthority("ROLE_${r.toString()}") }

                val resourceAccess = jwt.getClaim<Map<String, Any>?>("resource_access") ?: emptyMap()
                resourceAccess.values.forEach { entry ->
                    val roles = (entry as? Map<*, *>)?.get("roles") as? Collection<*>
                    roles?.forEach { r -> authorities += SimpleGrantedAuthority("ROLE_${r.toString()}") }
                }

                authorities
            }
        }
        // ⬇️ 어댑터는 Converter<Jwt, Mono<AbstractAuthenticationToken>> 를 구현
        return ReactiveJwtAuthenticationConverterAdapter(delegate)
    }
}

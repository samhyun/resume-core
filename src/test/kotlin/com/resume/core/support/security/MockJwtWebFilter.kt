package com.resume.core.support.security

import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * WebFilter for injecting mock JWT into reactive security context
 */
class MockJwtWebFilter(private val jwt: Jwt) : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authentication = JwtAuthenticationToken(jwt)
        val securityContext = SecurityContextImpl(authentication)

        return chain.filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
    }

    companion object {
        /**
         * Create a WebFilter with a mock JWT for the given user
         */
        fun create(subject: String = "test-user", claims: Map<String, Any> = emptyMap()): MockJwtWebFilter {
            val jwt = MockJwtFactory.createMockJwt(subject, claims)
            return MockJwtWebFilter(jwt)
        }

        /**
         * Configure WebTestClient with mock JWT authentication using mutateWith
         */
        fun asMutator(subject: String = "test-user", claims: Map<String, Any> = emptyMap()) =
            SecurityMockServerConfigurers.mockJwt().jwt(MockJwtFactory.createMockJwt(subject, claims))
    }
}
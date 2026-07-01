package com.resume.core.adapter.inbound.web.support

import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Provides access to the authenticated user's JWT and identifier from the reactive security context.
 */
@Component
class ReactiveJwtAuthenticationFacade {

    private val authenticationRequired = ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required")

    fun currentJwt(): Mono<Jwt> =
        ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val principal = context.authentication?.principal
                if (principal is Jwt) {
                    Mono.just(principal)
                } else {
                    Mono.empty()
                }
            }
            .switchIfEmpty(Mono.error(authenticationRequired))

    fun currentUserId(): Mono<String> =
        currentJwt()
            .map { jwt ->
                jwt.subject?.takeIf { it.isNotBlank() }
                    ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID not found in token")
            }

    /**
     * The raw access-token string of the current request — forwarded to downstream identity-provider
     * calls (e.g. the Keycloak Account API) so they act as the authenticated user.
     */
    fun currentToken(): Mono<String> =
        currentJwt().map { it.tokenValue }
}

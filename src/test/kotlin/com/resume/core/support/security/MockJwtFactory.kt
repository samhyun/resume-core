package com.resume.core.support.security

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

/**
 * Factory for creating mock JWT tokens for testing
 */
object MockJwtFactory {

    /**
     * Create a mock JWT with the given subject (userId)
     */
    fun createMockJwt(
        subject: String = "test-user",
        claims: Map<String, Any> = emptyMap()
    ): Jwt {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plusSeconds(3600)

        val allClaims = mutableMapOf<String, Any>(
            "sub" to subject,
            "iat" to issuedAt.epochSecond,
            "exp" to expiresAt.epochSecond,
            "iss" to "test-issuer",
            "aud" to listOf("test-audience")
        )
        allClaims.putAll(claims)

        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .header("typ", "JWT")
            .claims { it.putAll(allClaims) }
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(subject)
            .build()
    }
}
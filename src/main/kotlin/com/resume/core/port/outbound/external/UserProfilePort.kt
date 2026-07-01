package com.resume.core.port.outbound.external

import com.resume.core.domain.model.UserProfile
import reactor.core.publisher.Mono

/**
 * Port for reading/updating the authenticated user's profile in the identity provider (Keycloak).
 * The user's own access token authorizes the call, so it is always scoped to that user.
 */
interface UserProfilePort {
    fun getProfile(accessToken: String): Mono<UserProfile>

    fun updateProfile(accessToken: String, firstName: String?, lastName: String?): Mono<UserProfile>
}

package com.resume.core.port.outbound.persistence

import com.resume.core.domain.model.UserProfile
import reactor.core.publisher.Mono

/**
 * Port for reading/updating the authenticated user's profile in the `app_user` store.
 * All operations are keyed by username (from the JWT) — there is no cross-user access.
 */
interface UserProfileRepositoryPort {
    fun findByUsername(username: String): Mono<UserProfile>

    /** Update display fields; returns the updated profile, or empty if no such user. */
    fun updateProfile(
        username: String,
        displayName: String?,
        firstName: String?,
        lastName: String?
    ): Mono<UserProfile>
}

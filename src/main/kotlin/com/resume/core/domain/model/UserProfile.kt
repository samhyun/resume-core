package com.resume.core.domain.model

/**
 * The authenticated user's profile, sourced from Keycloak (the identity provider) — users are
 * managed in Keycloak, not in a local table. Editable fields are [firstName]/[lastName];
 * [username]/[email]/[emailVerified] are read-only here.
 */
data class UserProfile(
    val username: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val emailVerified: Boolean
)

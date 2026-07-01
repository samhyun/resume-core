package com.resume.core.domain.model

/**
 * The authenticated user's editable profile, backed by the `app_user` table (the Keycloak
 * user store). Identity/security fields (username, password, email verification) are not part
 * of this model — only the display fields a user may freely edit.
 */
data class UserProfile(
    val username: String,
    val email: String,
    val displayName: String?,
    val firstName: String?,
    val lastName: String?,
    val emailVerified: Boolean
)

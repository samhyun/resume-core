package com.resume.core.adapter.inbound.web.model

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.domain.model.UserProfile

/**
 * Profile edit request — full replacement of the editable name fields (blank → cleared).
 * Only `firstName`/`lastName` are editable here; username/email are managed in Keycloak.
 * (Keycloak validates the fields; invalid values surface as its 4xx.)
 */
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null
) {
    fun toCommand(): UpdateUserProfileCommand =
        UpdateUserProfileCommand(
            firstName = firstName?.trim()?.takeIf { it.isNotEmpty() },
            lastName = lastName?.trim()?.takeIf { it.isNotEmpty() }
        )
}

data class ProfileResponse(
    val username: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val emailVerified: Boolean
) {
    companion object {
        fun from(profile: UserProfile): ProfileResponse =
            ProfileResponse(
                username = profile.username,
                email = profile.email,
                firstName = profile.firstName,
                lastName = profile.lastName,
                emailVerified = profile.emailVerified
            )
    }
}

package com.resume.core.adapter.inbound.web.model

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.domain.model.UserProfile
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Profile edit request — **full replacement** of the editable display fields. The client should
 * send the complete set (typically pre-filled from `GET /profile`); a field sent blank/omitted is
 * cleared (set to null). Username/email/password are not editable here (identity/security flows).
 */
data class UpdateProfileRequest(
    val displayName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null
) {
    fun toCommand(username: String): UpdateUserProfileCommand =
        UpdateUserProfileCommand(
            username = username,
            displayName = displayName.normalized("displayName"),
            firstName = firstName.normalized("firstName"),
            lastName = lastName.normalized("lastName")
        )

    /** Trim, treat blank as null (cleared), and reject values over the column length as 400. */
    private fun String?.normalized(field: String): String? {
        val value = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (value.length > MAX_FIELD_LENGTH) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "$field must be at most $MAX_FIELD_LENGTH characters"
            )
        }
        return value
    }

    private companion object {
        const val MAX_FIELD_LENGTH = 100
    }
}

data class ProfileResponse(
    val username: String,
    val email: String,
    val displayName: String?,
    val firstName: String?,
    val lastName: String?,
    val emailVerified: Boolean
) {
    companion object {
        fun from(profile: UserProfile): ProfileResponse =
            ProfileResponse(
                username = profile.username,
                email = profile.email,
                displayName = profile.displayName,
                firstName = profile.firstName,
                lastName = profile.lastName,
                emailVerified = profile.emailVerified
            )
    }
}

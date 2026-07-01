package com.resume.core.application.dto.write

data class UpdateUserProfileCommand(
    val username: String,
    val displayName: String?,
    val firstName: String?,
    val lastName: String?
)

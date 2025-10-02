package com.resume.core.application.dto.write

import com.fasterxml.jackson.annotation.JsonValue


enum class SessionPurpose(@get:JsonValue val value: String) {
    RESUME_UPGRADE("resume_upgrade"),
    INTERVIEW_PREP("interview_prep"),
    GENERAL("general");
}


data class SessionIds(
    val appName: String,
    val userId: String,
    val sessionId: String
)


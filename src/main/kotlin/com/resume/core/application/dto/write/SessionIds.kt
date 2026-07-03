package com.resume.core.application.dto.write

/**
 * ADK 에이전트 세션을 가리키는 식별자 3종 (`/apps/{app}/users/{user}/sessions/{session}`).
 */
data class SessionIds(
    val appName: String,
    val userId: String,
    val sessionId: String,
)

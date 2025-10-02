package com.resume.core.application.dto.write

import java.util.UUID

data class CreateChatSessionResult(
    val sessionId: UUID,
    val agentSessionId: String,
    val appName: String,
    val userId: String,
    val purpose: String?,
    val status: String = "ACTIVE"
)

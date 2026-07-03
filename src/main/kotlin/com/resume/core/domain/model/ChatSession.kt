package com.resume.core.domain.model

import java.time.Instant
import java.util.UUID

/**
 * 로컬 사용자와 외부 AI 에이전트 세션을 잇는 채팅 세션.
 * 사용자당 ACTIVE 세션은 최대 1개이며, 새 세션을 만들면 기존 세션은 닫힌다.
 */
data class ChatSession(
    val id: UUID,
    val userId: String,
    val agentSessionId: String,
    val appName: String,
    val stateJson: String,
    val purpose: String?,
    val status: String,
    val lastUpdateTime: Double?,
    val createdAt: Instant = Instant.now(),
    val endedAt: Instant? = null,
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_CLOSED = "CLOSED"
    }
}

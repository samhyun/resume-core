package com.resume.core.port.outbound.external

import com.resume.core.application.dto.write.CreateSessionCommand
import reactor.core.publisher.Mono

interface AiAgentPort {
    fun createSession(req: CreateSessionCommand): Mono<AiAgentSession>
}

data class AiAgentSession(
    val agentSessionId: String,
    val appName: String,
    val userId: String,
    val stateJson: String,
    val purpose: String?,
    val lastUpdateTime: Double?
)

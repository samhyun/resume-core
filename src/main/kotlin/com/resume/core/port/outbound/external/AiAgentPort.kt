package com.resume.core.port.outbound.external

import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.RunAgentSessionCommand
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AiAgentPort {
    fun createSession(req: CreateSessionCommand): Mono<AiAgentSession>
    fun runSession(req: RunAgentSessionCommand): Flux<AiAgentStreamEvent>
}

data class AiAgentSession(
    val agentSessionId: String,
    val appName: String,
    val userId: String,
    val stateJson: String,
    val purpose: String?,
    val lastUpdateTime: Double?
)

data class AiAgentStreamEvent(
    val id: String? = null,
    val event: String? = null,
    val data: String? = null,
    val retry: Long? = null,
    val comment: String? = null
)

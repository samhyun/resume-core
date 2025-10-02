package com.resume.core.adapter.inbound.web

import com.resume.core.application.dto.write.CreateChatSessionResult
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.SessionIds
import com.resume.core.application.dto.write.SessionPurpose
import com.resume.core.application.usecase.write.CreateChatSessionUseCase
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/chat-sessions")
class ChatSessionController(
    private val createChatSessionUseCase: CreateChatSessionUseCase
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateChatSessionRequest): Mono<CreateChatSessionResponse> =
        createChatSessionUseCase
            .handle(request.toCommand())
            .map(CreateChatSessionResponse::from)
}

data class CreateChatSessionRequest(
    val appName: String,
    val userId: String,
    val purpose: SessionPurpose? = null
) {
    fun toCommand(): CreateSessionCommand =
        CreateSessionCommand(
            ids = SessionIds(
                appName = appName,
                userId = userId,
                sessionId = generateSessionId()
            ),
            purpose = purpose ?: SessionPurpose.GENERAL
        )
}

data class CreateChatSessionResponse(
    val sessionId: UUID,
    val agentSessionId: String,
    val appName: String,
    val userId: String,
    val purpose: String?,
    val status: String
) {
    companion object {
        fun from(result: CreateChatSessionResult): CreateChatSessionResponse =
            CreateChatSessionResponse(
                sessionId = result.sessionId,
                agentSessionId = result.agentSessionId,
                appName = result.appName,
                userId = result.userId,
                purpose = result.purpose,
                status = result.status
            )
    }
}

private fun generateSessionId(): String = "session-${UUID.randomUUID()}"

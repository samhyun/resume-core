package com.resume.core.application.usecase.read

import com.resume.core.application.dto.write.AgentFunctionResponse
import com.resume.core.application.dto.write.AgentInlineData
import com.resume.core.application.dto.write.AgentMessage
import com.resume.core.application.dto.write.AgentMessagePart
import com.resume.core.application.dto.write.ChatMessagePayload
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import com.resume.core.port.outbound.persistence.ChatSessionRepositoryPort
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.Base64

@Service
class StreamChatSessionUseCaseService(
    private val agent: AiAgentPort,
    private val chatSessionRepository: ChatSessionRepositoryPort,
) : StreamChatSessionUseCase {

    override fun stream(command: RunChatSessionCommand): Flux<AiAgentStreamEvent> =
        // 세션 ID 단독이 아니라 (세션 ID + 인증 사용자)로 조회해 타 사용자 세션 접근을 차단한다.
        chatSessionRepository.findByIdAndUserId(command.sessionId, command.userId)
            .switchIfEmpty(
                Mono.error(
                    ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Chat session ${command.sessionId} not found"
                    )
                )
            )
            .flatMapMany { session ->
                command.message
                    .toAgentMessage()
                    .flatMapMany { agentMessage ->
                        agent.runSession(
                            RunAgentSessionCommand(
                                appName = session.appName,
                                userId = session.userId,
                                sessionId = session.agentSessionId,
                                newMessage = agentMessage
                            )
                        )
                    }
            }

    private fun ChatMessagePayload.toAgentMessage(): Mono<AgentMessage> = when (this) {
        is ChatMessagePayload.Text -> Mono.just(
            AgentMessage(
                role = "user",
                parts = listOf(AgentMessagePart(text = text))
            )
        )
        // HITL 재개: function_response 단독 파트(text와 섞지 않음). response 는 {"result": <답변>}.
        is ChatMessagePayload.FunctionResponse -> Mono.just(
            AgentMessage(
                role = "user",
                parts = listOf(
                    AgentMessagePart(
                        functionResponse = AgentFunctionResponse(
                            id = id,
                            name = name,
                            response = mapOf("result" to result)
                        )
                    )
                )
            )
        )
        is ChatMessagePayload.File -> DataBufferUtils.join(part.content())
            .handle { buffer: DataBuffer, sink ->
                val bytes = ByteArray(buffer.readableByteCount())
                buffer.read(bytes)
                DataBufferUtils.release(buffer)
                if (bytes.isEmpty()) {
                    sink.error(
                        ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "File data must not be empty"
                        )
                    )
                    return@handle
                }
                sink.next(Base64.getEncoder().encodeToString(bytes))
            }
            .map { encoded ->
                AgentMessage(
                    role = "user",
                    parts = listOf(
                        AgentMessagePart(
                            inlineData = AgentInlineData(
                                displayName = displayName,
                                data = encoded,
                                mimeType = mimeType
                            )
                        )
                    )
                )
            }
    }
}

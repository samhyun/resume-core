package com.resume.core.application.usecase.read

import com.resume.core.adapter.outbound.persistence.command.repository.ChatSessionRepository
import com.resume.core.application.dto.write.AgentFunctionResponse
import com.resume.core.application.dto.write.AgentInlineData
import com.resume.core.application.dto.write.AgentMessage
import com.resume.core.application.dto.write.AgentMessagePart
import com.resume.core.application.dto.write.ChatMessagePayload
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentStreamEvent
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
    private val chatSessionRepository: ChatSessionRepository
) : StreamChatSessionUseCase {

    override fun stream(command: RunChatSessionCommand): Flux<AiAgentStreamEvent> =
        chatSessionRepository.findById(command.sessionId)
            .switchIfEmpty(
                Mono.error(
                    ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Chat session ${command.sessionId} not found"
                    )
                )
            )
            .flatMapMany { entity ->
                command.message
                    .toAgentMessage()
                    .flatMapMany { agentMessage ->
                        agent.runSession(
                            RunAgentSessionCommand(
                                appName = entity.appName,
                                userId = entity.userId,
                                sessionId = entity.agentSessionId,
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

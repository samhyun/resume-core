package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.CreateChatSessionResult
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.domain.model.ChatSession
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.persistence.ChatSessionRepositoryPort
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
class CreateChatSessionUseCaseService(
    private val agent: AiAgentPort,
    private val chatSessionRepository: ChatSessionRepositoryPort,
) : CreateChatSessionUseCase {

    override fun handle(command: CreateSessionCommand): Mono<CreateChatSessionResult> =
        agent.createSession(command)
            .flatMap { session ->
                // 소유자/앱 이름은 업스트림 응답(session.userId/appName)이 아니라
                // 인증된 요청 값(JWT sub 기반 command.ids)을 신뢰한다 — 잘못된 소유자 저장 방지.
                val chatSession = ChatSession(
                    id = UUID.randomUUID(),
                    userId = command.ids.userId,
                    agentSessionId = session.agentSessionId,
                    appName = command.ids.appName,
                    stateJson = session.stateJson,
                    purpose = session.purpose,
                    status = ChatSession.STATUS_ACTIVE,
                    lastUpdateTime = session.lastUpdateTime,
                )

                // 기존 ACTIVE 종료 + 새 세션 저장을 원자적으로 처리한다.
                chatSessionRepository.replaceActiveSession(chatSession)
                    .map { saved ->
                        CreateChatSessionResult(
                            sessionId = saved.id,
                            agentSessionId = saved.agentSessionId,
                            appName = saved.appName,
                            userId = saved.userId,
                            purpose = saved.purpose,
                            status = saved.status,
                        )
                    }
            }
}

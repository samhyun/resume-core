package com.resume.core.application.usecase.write

import com.resume.core.adapter.outbound.persistence.command.entity.ChatSessionEntity
import com.resume.core.adapter.outbound.persistence.command.repository.ChatSessionRepository
import com.resume.core.application.dto.write.CreateChatSessionResult
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.port.outbound.external.AiAgentPort
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID
import io.r2dbc.postgresql.codec.Json

@Service
class CreateChatSessionUseCaseService(
    private val agent: AiAgentPort,
    private val repo: ChatSessionRepository
) : CreateChatSessionUseCase {


    override fun handle(req: CreateSessionCommand): Mono<CreateChatSessionResult> =
        agent.createSession(req)
            .flatMap { session ->
                val localId = UUID.randomUUID()

                val entity = ChatSessionEntity(
                    id = localId,
                    userId = session.userId,
                    agentSessionId = session.agentSessionId,
                    appName = session.appName,
                    state = Json.of(session.stateJson),
                    purpose = session.purpose,
                    status = "ACTIVE",
                    lastUpdateTime = session.lastUpdateTime
                ).markNew()

                // 1) 기존 ACTIVE 종료 → 2) 새 세션 저장
                repo.closeActiveByUser(session.userId)
                    .then(repo.save(entity))
                    .thenReturn(
                        CreateChatSessionResult(
                            sessionId = localId,
                            agentSessionId = session.agentSessionId,
                            appName = session.appName,
                            userId = session.userId,
                            purpose = session.purpose,
                            status = "ACTIVE"
                        )
                    )
            }


}

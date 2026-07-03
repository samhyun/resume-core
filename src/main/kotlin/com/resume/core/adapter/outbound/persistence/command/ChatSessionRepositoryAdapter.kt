package com.resume.core.adapter.outbound.persistence.command

import com.resume.core.adapter.outbound.persistence.command.entity.ChatSessionEntity
import com.resume.core.adapter.outbound.persistence.command.repository.ChatSessionRepository
import com.resume.core.domain.model.ChatSession
import com.resume.core.port.outbound.persistence.ChatSessionRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * ChatSessionRepositoryPort 의 R2DBC 구현.
 * 도메인 ChatSession ↔ 엔티티 매핑과 Persistable INSERT 마킹을 담당한다.
 */
@Component
class ChatSessionRepositoryAdapter(
    private val repository: ChatSessionRepository,
    private val transactionalOperator: TransactionalOperator,
) : ChatSessionRepositoryPort {

    override fun replaceActiveSession(session: ChatSession): Mono<ChatSession> {
        // 기존 ACTIVE 종료 → 새 세션 INSERT 를 한 트랜잭션으로 묶는다.
        val operation = repository.closeActiveByUser(session.userId)
            .then(repository.save(ChatSessionEntity.from(session).markNew()))
            .map { it.toDomain() }

        return transactionalOperator.transactional(operation)
    }

    override fun findByIdAndUserId(sessionId: UUID, userId: String): Mono<ChatSession> =
        repository.findByIdAndUserId(sessionId, userId)
            .map { it.toDomain() }
}

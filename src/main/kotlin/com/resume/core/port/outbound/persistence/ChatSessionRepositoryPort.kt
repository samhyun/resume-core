package com.resume.core.port.outbound.persistence

import com.resume.core.domain.model.ChatSession
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * 채팅 세션 영속화 포트. 어댑터가 구현해 저장소 역량을 제공한다.
 */
interface ChatSessionRepositoryPort {
    /**
     * 해당 사용자의 기존 ACTIVE 세션을 모두 닫고 새 세션을 저장한다 (하나의 트랜잭션).
     * 중간 실패 시 사용자의 ACTIVE 세션이 0개로 남지 않도록 원자적으로 처리한다.
     */
    fun replaceActiveSession(session: ChatSession): Mono<ChatSession>

    /**
     * 소유 사용자로 스코프된 세션 단건 조회
     * 없으면 empty Mono 반환
     */
    fun findByIdAndUserId(sessionId: UUID, userId: String): Mono<ChatSession>
}

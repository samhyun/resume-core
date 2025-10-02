package com.resume.core.adapter.outbound.persistence.command.repository

import com.resume.core.adapter.outbound.persistence.command.entity.ChatSessionEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.*

@Repository
interface ChatSessionRepository: ReactiveCrudRepository<ChatSessionEntity, UUID> {

    @Query("""
    UPDATE chat_session 
       SET status='CLOSED', ended_at=now()
     WHERE user_id = :userId AND status='ACTIVE'
  """)
    fun closeActiveByUser(userId: String): Mono<Int>  // rowsUpdated

    @Query("""
    SELECT * FROM chat_session 
     WHERE user_id = :userId AND status='ACTIVE'
     ORDER BY created_at DESC
     LIMIT 1
  """)
    fun findActiveByUser(userId: String): Mono<ChatSessionEntity>
}
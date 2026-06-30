package com.resume.core.adapter.outbound.persistence.command.repository

import com.resume.core.adapter.outbound.persistence.command.entity.CoverLetterEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * R2DBC repository for cover letter persistence. All reads are scoped by userId for ownership.
 */
@Repository
interface CoverLetterRepository : ReactiveCrudRepository<CoverLetterEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: String): Mono<CoverLetterEntity>

    @Query(
        """
        SELECT * FROM cover_letter
        WHERE user_id = :userId
        ORDER BY created_at DESC
        """
    )
    fun findAllByUserId(userId: String): Flux<CoverLetterEntity>

    fun existsByIdAndUserId(id: UUID, userId: String): Mono<Boolean>

    @Query(
        """
        DELETE FROM cover_letter
        WHERE id = :id AND user_id = :userId
        """
    )
    fun deleteByIdAndUserId(id: UUID, userId: String): Mono<Void>
}

package com.resume.core.adapter.outbound.persistence.command.repository

import com.resume.core.adapter.outbound.persistence.command.entity.ResumeEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * R2DBC repository for resume persistence
 * Provides reactive database operations for resume entities
 */
@Repository
interface ResumeRepository : ReactiveCrudRepository<ResumeEntity, UUID> {

    /**
     * Find a resume by ID and user ID
     * Ensures users can only access their own resumes
     */
    fun findByIdAndUserId(id: UUID, userId: String): Mono<ResumeEntity>

    /**
     * Find the active resume for a user
     * Returns the most recently created active resume
     */
    @Query("""
        SELECT * FROM resume
        WHERE user_id = :userId AND is_active = true
        ORDER BY created_at DESC
        LIMIT 1
    """)
    fun findActiveByUserId(userId: String): Mono<ResumeEntity>

    /**
     * Retrieve all resumes for a user ordered by creation time descending
     */
    @Query(
        """
        SELECT * FROM resume
        WHERE user_id = :userId
        ORDER BY created_at DESC
        """
    )
    fun findAllByUserId(userId: String): Flux<ResumeEntity>

    /**
     * Deactivate all active resumes for a user
     * Used before saving a new resume to ensure only one active resume
     */
    @Query("""
        UPDATE resume
        SET is_active = false
        WHERE user_id = :userId AND is_active = true
    """)
    fun deactivateAllByUserId(userId: String): Mono<Int>

    /**
     * Check if a resume exists for the given ID and user
     */
    fun existsByIdAndUserId(id: UUID, userId: String): Mono<Boolean>
}

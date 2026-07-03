package com.resume.core.port.outbound.persistence

import com.resume.core.domain.model.Resume
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Port for resume persistence operations
 * Adapters implement this interface to provide resume storage capabilities
 */
interface ResumeRepositoryPort {
    /**
     * Save a new resume or update an existing one
     * Deactivates any existing active resume for the user before saving
     */
    fun save(resume: Resume): Mono<Resume>

    /**
     * Update an existing resume
     * Does not change active status; preserves createdAt while refreshing update metadata
     */
    fun update(resume: Resume): Mono<Resume>

    /**
     * Retrieve all resumes for a user ordered by creation date desc
     */
    fun findAllByUserId(userId: String): Flux<Resume>

    /**
     * Find a resume by its ID and user ID
     * Returns empty Mono if not found
     */
    fun findByIdAndUserId(resumeId: UUID, userId: String): Mono<Resume>

    /**
     * Find the active resume for a user
     * Returns empty Mono if no active resume exists
     */
    fun findActiveByUserId(userId: String): Mono<Resume>

    /**
     * Deactivate all active resumes for a user
     * Used before saving a new resume to ensure only one active resume per user
     */
    fun deactivateAllByUserId(userId: String): Mono<Unit>

    /**
     * Check if a resume exists for the given ID and user
     */
    fun existsByIdAndUserId(resumeId: UUID, userId: String): Mono<Boolean>

    /**
     * Delete a resume by ID scoped to the given user
     */
    fun deleteByIdAndUserId(resumeId: UUID, userId: String): Mono<Unit>
}

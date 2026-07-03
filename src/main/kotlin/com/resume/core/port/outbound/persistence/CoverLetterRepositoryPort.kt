package com.resume.core.port.outbound.persistence

import com.resume.core.domain.model.CoverLetter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Port for cover letter persistence. Adapters implement this to provide storage.
 */
interface CoverLetterRepositoryPort {
    /** Insert a new cover letter. */
    fun save(coverLetter: CoverLetter): Mono<CoverLetter>

    /** Update an existing cover letter (content/metadata edit); preserves createdAt. */
    fun update(coverLetter: CoverLetter): Mono<CoverLetter>

    /** All cover letters for a user, newest first. */
    fun findAllByUserId(userId: String): Flux<CoverLetter>

    /** A single cover letter by id scoped to the user; empty when not found. */
    fun findByIdAndUserId(id: UUID, userId: String): Mono<CoverLetter>

    fun existsByIdAndUserId(id: UUID, userId: String): Mono<Boolean>

    fun deleteByIdAndUserId(id: UUID, userId: String): Mono<Unit>
}

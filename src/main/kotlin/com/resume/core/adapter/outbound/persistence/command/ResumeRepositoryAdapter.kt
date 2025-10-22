package com.resume.core.adapter.outbound.persistence.command

import com.resume.core.adapter.outbound.persistence.command.entity.ResumeEntity
import com.resume.core.adapter.outbound.persistence.command.repository.ResumeRepository
import com.resume.core.domain.model.Resume
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Adapter implementing ResumeRepositoryPort using R2DBC
 * Provides reactive resume persistence operations
 */
@Component
class ResumeRepositoryAdapter(
    private val repository: ResumeRepository
) : ResumeRepositoryPort {

    override fun save(resume: Resume): Mono<Resume> {
        // First deactivate all active resumes for the user
        return repository.deactivateAllByUserId(resume.userId)
            .then(
                Mono.defer {
                    val entity = ResumeEntity.from(resume).markNew()
                    repository.save(entity)
                }
            )
            .map { it.toDomain() }
    }

    override fun findByIdAndUserId(resumeId: UUID, userId: String): Mono<Resume> =
        repository.findByIdAndUserId(resumeId, userId)
            .map { it.toDomain() }

    override fun findActiveByUserId(userId: String): Mono<Resume> =
        repository.findActiveByUserId(userId)
            .map { it.toDomain() }

    override fun deactivateAllByUserId(userId: String): Mono<Unit> =
        repository.deactivateAllByUserId(userId)
            .thenReturn(Unit)

    override fun existsByIdAndUserId(resumeId: UUID, userId: String): Mono<Boolean> =
        repository.existsByIdAndUserId(resumeId, userId)
}

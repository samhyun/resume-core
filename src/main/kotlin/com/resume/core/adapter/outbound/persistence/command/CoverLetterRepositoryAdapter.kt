package com.resume.core.adapter.outbound.persistence.command

import com.resume.core.adapter.outbound.persistence.command.entity.CoverLetterEntity
import com.resume.core.adapter.outbound.persistence.command.repository.CoverLetterRepository
import com.resume.core.domain.model.CoverLetter
import com.resume.core.port.outbound.persistence.CoverLetterRepositoryPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * R2DBC adapter for [CoverLetterRepositoryPort]. Save is a plain insert (no single-active concept).
 */
@Component
class CoverLetterRepositoryAdapter(
    private val repository: CoverLetterRepository
) : CoverLetterRepositoryPort {

    override fun save(coverLetter: CoverLetter): Mono<CoverLetter> {
        val entity = CoverLetterEntity.from(coverLetter).markNew()
        return repository.save(entity).map { it.toDomain() }
    }

    override fun update(coverLetter: CoverLetter): Mono<CoverLetter> {
        val entity = CoverLetterEntity.from(coverLetter).markPersisted()
        return repository.save(entity)
            .flatMap { repository.findByIdAndUserId(coverLetter.id, coverLetter.userId) }
            .map { it.toDomain() }
    }

    override fun findAllByUserId(userId: String): Flux<CoverLetter> =
        repository.findAllByUserId(userId).map { it.toDomain() }

    override fun findByIdAndUserId(id: UUID, userId: String): Mono<CoverLetter> =
        repository.findByIdAndUserId(id, userId).map { it.toDomain() }

    override fun existsByIdAndUserId(id: UUID, userId: String): Mono<Boolean> =
        repository.existsByIdAndUserId(id, userId)

    override fun deleteByIdAndUserId(id: UUID, userId: String): Mono<Unit> =
        repository.deleteByIdAndUserId(id, userId).thenReturn(Unit)
}

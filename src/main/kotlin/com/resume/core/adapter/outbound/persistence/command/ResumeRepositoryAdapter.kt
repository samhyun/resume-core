package com.resume.core.adapter.outbound.persistence.command

import com.resume.core.adapter.outbound.persistence.command.entity.ResumeEntity
import com.resume.core.adapter.outbound.persistence.command.repository.ResumeRepository
import com.resume.core.domain.model.Resume
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * ResumeRepositoryPort 의 R2DBC 구현.
 * 리액티브 이력서 영속화를 담당한다.
 */
@Component
class ResumeRepositoryAdapter(
    private val repository: ResumeRepository,
    private val transactionalOperator: TransactionalOperator,
) : ResumeRepositoryPort {

    override fun save(resume: Resume): Mono<Resume> {
        // 기존 활성 이력서 비활성화 → 새 이력서 저장을 하나의 트랜잭션으로 묶는다.
        // (묶지 않으면 insert 실패 시 사용자가 활성 이력서 0개 상태로 남을 수 있다.)
        val operation = repository.deactivateAllByUserId(resume.userId)
            .then(
                Mono.defer {
                    val entity = ResumeEntity.from(resume).markNew()
                    repository.save(entity)
                }
            )
            .map { it.toDomain() }

        return transactionalOperator.transactional(operation)
    }

    override fun update(resume: Resume): Mono<Resume> {
        val entity = ResumeEntity.from(resume).markPersisted()

        return repository.save(entity)
            .flatMap {
                repository.findByIdAndUserId(resume.id, resume.userId)
            }
            .map { it.toDomain() }
    }

    override fun findAllByUserId(userId: String) =
        repository.findAllByUserId(userId)
            .map { it.toDomain() }

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

    override fun deleteByIdAndUserId(resumeId: UUID, userId: String): Mono<Unit> =
        repository.deleteByIdAndUserId(resumeId, userId)
            .thenReturn(Unit)
}

package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GetActiveResumeQuery
import com.resume.core.application.dto.read.GetResumeQuery
import com.resume.core.domain.model.Resume
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Service implementing GetResumeUseCase
 * Retrieves a specific resume by ID
 */
@Service
class GetResumeUseCaseService(
    private val resumeRepository: ResumeRepositoryPort
) : GetResumeUseCase {

    override fun handle(query: GetResumeQuery): Mono<Resume> =
        resumeRepository.findByIdAndUserId(query.resumeId, query.userId)
}

/**
 * Service implementing GetActiveResumeUseCase
 * Retrieves the user's currently active resume
 */
@Service
class GetActiveResumeUseCaseService(
    private val resumeRepository: ResumeRepositoryPort
) : GetActiveResumeUseCase {

    override fun handle(query: GetActiveResumeQuery): Mono<Resume> =
        resumeRepository.findActiveByUserId(query.userId)
}
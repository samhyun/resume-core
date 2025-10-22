package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GetActiveResumeQuery
import com.resume.core.application.dto.read.GetResumeQuery
import com.resume.core.domain.model.Resume
import reactor.core.publisher.Mono

/**
 * Use case interface for retrieving a specific resume
 */
fun interface GetResumeUseCase {
    fun handle(query: GetResumeQuery): Mono<Resume>
}

/**
 * Use case interface for retrieving user's active resume
 */
fun interface GetActiveResumeUseCase {
    fun handle(query: GetActiveResumeQuery): Mono<Resume>
}
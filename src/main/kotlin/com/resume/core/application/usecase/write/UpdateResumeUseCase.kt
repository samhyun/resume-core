package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.UpdateResumeCommand
import com.resume.core.application.dto.write.UpdateResumeResult
import reactor.core.publisher.Mono

/**
 * Use case for updating an existing resume
 */
fun interface UpdateResumeUseCase {
    fun handle(command: UpdateResumeCommand): Mono<UpdateResumeResult>
}

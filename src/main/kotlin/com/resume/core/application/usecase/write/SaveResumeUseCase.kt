package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.SaveResumeCommand
import com.resume.core.application.dto.write.SaveResumeResult
import reactor.core.publisher.Mono

/**
 * Use case interface for saving a resume
 * Handles both new resume creation and updates to existing resumes
 */
fun interface SaveResumeUseCase {
    fun handle(command: SaveResumeCommand): Mono<SaveResumeResult>
}
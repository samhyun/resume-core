package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.GenerateCoverLetterCommand
import com.resume.core.application.dto.write.GenerateCoverLetterResult
import reactor.core.publisher.Mono

/**
 * Generates a cover letter by orchestrating the cover_letter ADK agent server-side
 * (create session → trigger → auto-answer the HITL company-info prompt → read final state).
 */
interface GenerateCoverLetterUseCase {
    fun handle(command: GenerateCoverLetterCommand): Mono<GenerateCoverLetterResult>
}

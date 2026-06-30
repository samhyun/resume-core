package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.GenerateCoverLetterCommand
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import reactor.core.publisher.Flux

/**
 * Generates a cover letter by orchestrating the cover_letter ADK agent server-side, then
 * **streaming** the result back.
 *
 * Core injects `resume_data`, auto-answers the single `cl_company_info` HITL prompt with the
 * form data, and then relays the resulting pipeline SSE straight to the caller (no blocking
 * aggregation — so long generations don't trip request timeouts). The frontend consumes the
 * stream exactly like the chat run-sse (filter hidden events, read the final text).
 */
interface GenerateCoverLetterUseCase {
    fun stream(command: GenerateCoverLetterCommand): Flux<AiAgentStreamEvent>
}

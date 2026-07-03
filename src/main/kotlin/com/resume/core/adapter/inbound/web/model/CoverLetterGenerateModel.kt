package com.resume.core.adapter.inbound.web.model

import com.resume.core.application.dto.write.GenerateCoverLetterCommand
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * Request to generate a cover letter from a resume + target company/role.
 * `companyCulture` is optional extra context for the agent.
 */
data class GenerateCoverLetterRequest(
    val resumeId: UUID,
    val companyName: String,
    val position: String,
    val jobDescription: String? = null,
    val companyCulture: String? = null
) {
    fun toCommand(userId: String): GenerateCoverLetterCommand {
        // 빈 값으로 에이전트를 호출하면 품질 낮은 결과 + 불필요한 비용/지연이 발생하므로 거부한다.
        if (companyName.isBlank() || position.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "companyName and position must not be blank"
            )
        }
        return GenerateCoverLetterCommand(
            userId = userId,
            resumeId = resumeId,
            companyName = companyName.trim(),
            position = position.trim(),
            jobDescription = jobDescription?.trim()?.takeIf { it.isNotEmpty() },
            companyCulture = companyCulture?.trim()?.takeIf { it.isNotEmpty() }
        )
    }
}

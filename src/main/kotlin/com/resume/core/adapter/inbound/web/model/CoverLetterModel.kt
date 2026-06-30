package com.resume.core.adapter.inbound.web.model

import com.resume.core.application.dto.write.SaveCoverLetterCommand
import com.resume.core.application.dto.write.UpdateCoverLetterCommand
import com.resume.core.domain.model.CoverLetter
import java.time.LocalDateTime
import java.util.UUID

/**
 * Request body for creating (POST) or editing (PUT) a cover letter.
 * `validationScore`/`resumeId` are only meaningful on create (carried from the generation result).
 */
data class SaveCoverLetterRequest(
    val companyName: String,
    val position: String,
    val content: String,
    val resumeId: UUID? = null,
    val jobDescription: String? = null,
    val validationScore: Int? = null
) {
    fun toCommand(userId: String): SaveCoverLetterCommand =
        SaveCoverLetterCommand(
            userId = userId,
            resumeId = resumeId,
            companyName = companyName,
            position = position,
            jobDescription = jobDescription,
            content = content,
            validationScore = validationScore
        )

    fun toUpdateCommand(id: UUID, userId: String): UpdateCoverLetterCommand =
        UpdateCoverLetterCommand(
            id = id,
            userId = userId,
            companyName = companyName,
            position = position,
            jobDescription = jobDescription,
            content = content
        )
}

data class CoverLetterResponse(
    val id: UUID,
    val userId: String,
    val resumeId: UUID?,
    val companyName: String,
    val position: String,
    val jobDescription: String?,
    val content: String,
    val validationScore: Int?,
    val version: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(coverLetter: CoverLetter): CoverLetterResponse =
            CoverLetterResponse(
                id = coverLetter.id,
                userId = coverLetter.userId,
                resumeId = coverLetter.resumeId,
                companyName = coverLetter.companyName,
                position = coverLetter.position,
                jobDescription = coverLetter.jobDescription,
                content = coverLetter.content,
                validationScore = coverLetter.validationScore,
                version = coverLetter.version,
                createdAt = coverLetter.createdAt,
                updatedAt = coverLetter.updatedAt
            )
    }
}

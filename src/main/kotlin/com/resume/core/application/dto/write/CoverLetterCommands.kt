package com.resume.core.application.dto.write

import java.util.UUID

data class SaveCoverLetterCommand(
    val userId: String,
    val resumeId: UUID?,
    val companyName: String,
    val position: String,
    val jobDescription: String?,
    val content: String,
    val validationScore: Int?
)

data class UpdateCoverLetterCommand(
    val id: UUID,
    val userId: String,
    val companyName: String,
    val position: String,
    val jobDescription: String?,
    val content: String
)

data class DeleteCoverLetterCommand(
    val id: UUID,
    val userId: String
)

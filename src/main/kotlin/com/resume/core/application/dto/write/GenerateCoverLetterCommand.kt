package com.resume.core.application.dto.write

import java.util.UUID

data class GenerateCoverLetterCommand(
    val userId: String,
    val resumeId: UUID,
    val companyName: String,
    val position: String,
    val jobDescription: String?,
    val companyCulture: String?
)

/**
 * Generated draft returned to the client. Not persisted — the user saves it explicitly
 * via `POST /cover-letters`.
 */
data class GenerateCoverLetterResult(
    val resumeId: UUID,
    val companyName: String,
    val position: String,
    val jobDescription: String?,
    val content: String,
    val validationScore: Int?
)

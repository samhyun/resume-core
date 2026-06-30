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

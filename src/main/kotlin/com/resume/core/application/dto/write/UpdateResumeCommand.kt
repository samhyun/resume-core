package com.resume.core.application.dto.write

import com.resume.core.domain.model.ResumeData
import java.util.UUID

/**
 * Command for updating an existing resume
 */
data class UpdateResumeCommand(
    val resumeId: UUID,
    val userId: String,
    val resumeData: ResumeData
)

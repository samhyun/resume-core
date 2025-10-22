package com.resume.core.application.dto.write

import com.resume.core.domain.model.ResumeData

/**
 * Command for saving a resume
 * Used by the use case layer to handle resume save operations
 */
data class SaveResumeCommand(
    val userId: String,
    val resumeData: ResumeData
)
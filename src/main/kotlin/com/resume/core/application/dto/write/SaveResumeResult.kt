package com.resume.core.application.dto.write

import java.util.UUID

/**
 * Result of resume save operation
 * Contains the ID of the saved/updated resume
 */
data class SaveResumeResult(
    val resumeId: UUID,
    val userId: String,
    val version: Int
)
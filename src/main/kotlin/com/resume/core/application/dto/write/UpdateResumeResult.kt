package com.resume.core.application.dto.write

import java.util.UUID

/**
 * Result of updating an existing resume
 */
data class UpdateResumeResult(
    val resumeId: UUID,
    val userId: String,
    val version: Int
)

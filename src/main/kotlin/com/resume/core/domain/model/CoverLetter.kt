package com.resume.core.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model representing a generated/edited cover letter.
 *
 * Unlike [Resume] there is no single-active concept — a user keeps a collection of cover letters.
 * [content] is the free-text body (the agent's `draft_cover_letter.full_text`).
 */
data class CoverLetter(
    val id: UUID,
    val userId: String,
    /** Resume this cover letter was generated from; nullable since resumes can be deleted. */
    val resumeId: UUID?,
    val companyName: String,
    val position: String,
    val jobDescription: String?,
    val content: String,
    /** Agent validation score (`validation_result.total_score`); null when edited/saved manually. */
    val validationScore: Int?,
    val version: Int,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

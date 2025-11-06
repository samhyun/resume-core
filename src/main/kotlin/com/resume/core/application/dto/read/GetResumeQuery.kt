package com.resume.core.application.dto.read

import java.util.UUID

/**
 * Query for retrieving a resume
 */
data class GetResumeQuery(
    val resumeId: UUID,
    val userId: String
)

/**
 * Query for retrieving user's active resume
 */
data class GetActiveResumeQuery(
    val userId: String
)

/**
 * Query for retrieving all resumes owned by a user
 */
data class ListResumesQuery(
    val userId: String
)

package com.resume.core.application.dto.write

import java.util.UUID

data class DeleteResumeCommand(
    val resumeId: UUID,
    val userId: String
)

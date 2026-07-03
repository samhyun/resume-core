package com.resume.core.application.dto.read

import java.util.UUID

data class GetCoverLetterQuery(
    val id: UUID,
    val userId: String
)

data class ListCoverLettersQuery(
    val userId: String
)

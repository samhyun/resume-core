package com.resume.core.application.dto.read

import com.resume.core.domain.model.ResumeTemplateType
import java.util.UUID

/**
 * Query object for requesting a PDF rendering of a resume.
 */
data class GenerateResumePdfQuery(
    val resumeId: UUID,
    val userId: String,
    val templateType: ResumeTemplateType
)

/**
 * Result DTO containing PDF bytes and download metadata.
 */
data class ResumePdfResult(
    val fileName: String,
    val contentType: String = "application/pdf",
    val bytes: ByteArray
)

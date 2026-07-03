package com.resume.core.application.dto.read

import com.resume.core.domain.model.ResumeExportFormat
import com.resume.core.domain.model.ResumeTemplateType
import java.util.UUID

/**
 * Query object for exporting a resume in a given [format].
 *
 * [templateType] only affects HTML-based formats (PDF/PNG); it is ignored for TXT.
 */
data class GenerateResumeExportQuery(
    val resumeId: UUID,
    val userId: String,
    val templateType: ResumeTemplateType,
    val format: ResumeExportFormat
)

/**
 * Result DTO containing exported document bytes and download metadata.
 */
data class ResumeExportResult(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray
)

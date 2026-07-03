package com.resume.core.domain.model

/**
 * Supported resume export formats.
 *
 * - [htmlBased] formats are produced by rendering the Thymeleaf template to HTML and
 *   converting it with wkhtmltopdf / wkhtmltoimage.
 * - [TXT] bypasses HTML entirely and is rendered straight from [ResumeData] as plain text.
 */
enum class ResumeExportFormat(
    val value: String,
    val contentType: String,
    val extension: String,
    val htmlBased: Boolean
) {
    PDF("pdf", "application/pdf", "pdf", true),
    PNG("png", "image/png", "png", true),
    TXT("txt", "text/plain;charset=UTF-8", "txt", false);

    companion object {
        /** Strict parse: returns null for an unknown value so callers can reject it (vs. silently defaulting). */
        fun from(value: String): ResumeExportFormat? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

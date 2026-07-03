package com.resume.core.domain.model

/**
 * Supported resume templates for PDF generation
 */
enum class ResumeTemplateType(val templateName: String) {
    DEFAULT("default"),
    MODERN("modern"),
    MINIMALIST("minimalist");

    val templatePath: String = "resume/$templateName"

    companion object {
        fun fromValue(value: String?): ResumeTemplateType =
            value?.let { candidate ->
                entries.firstOrNull { it.templateName.equals(candidate, ignoreCase = true) }
            } ?: DEFAULT
    }
}

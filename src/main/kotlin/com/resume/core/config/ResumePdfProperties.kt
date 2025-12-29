package com.resume.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resume.pdf")
data class ResumePdfProperties(
    val wkhtmltopdfPath: String = "wkhtmltopdf",
    val timeoutSeconds: Long = 30
)

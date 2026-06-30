package com.resume.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "resume.pdf")
data class ResumePdfProperties(
    val wkhtmltopdfPath: String = "wkhtmltopdf",
    val wkhtmltoimagePath: String = "wkhtmltoimage",
    val timeoutSeconds: Long = 30,
    /** Fixed render width (px) for PNG export; single-page resumes flow downward at this width. */
    val pngWidth: Int = 1024
)

package com.resume.core.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResumeExportFormatTest {

    @Test
    fun `from parses known formats case-insensitively`() {
        assertThat(ResumeExportFormat.from("pdf")).isEqualTo(ResumeExportFormat.PDF)
        assertThat(ResumeExportFormat.from("PNG")).isEqualTo(ResumeExportFormat.PNG)
        assertThat(ResumeExportFormat.from("Txt")).isEqualTo(ResumeExportFormat.TXT)
    }

    @Test
    fun `from returns null for unknown format`() {
        assertThat(ResumeExportFormat.from("docx")).isNull()
        assertThat(ResumeExportFormat.from("")).isNull()
    }

    @Test
    fun `only TXT is non-html-based`() {
        assertThat(ResumeExportFormat.PDF.htmlBased).isTrue()
        assertThat(ResumeExportFormat.PNG.htmlBased).isTrue()
        assertThat(ResumeExportFormat.TXT.htmlBased).isFalse()
    }
}

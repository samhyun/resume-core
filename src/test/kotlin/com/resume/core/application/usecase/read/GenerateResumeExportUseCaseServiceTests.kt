package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GenerateResumeExportQuery
import com.resume.core.application.dto.read.ResumeExportResult
import com.resume.core.domain.model.*
import com.resume.core.port.outbound.external.DocumentConversionPort
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import com.resume.core.port.outbound.rendering.ResumePlainTextRendererPort
import com.resume.core.port.outbound.rendering.ResumeTemplateRendererPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.UUID

class GenerateResumeExportUseCaseServiceTests {

    private lateinit var resumeRepositoryPort: ResumeRepositoryPort
    private lateinit var resumeTemplateRendererPort: ResumeTemplateRendererPort
    private lateinit var plainTextRendererPort: ResumePlainTextRendererPort
    private lateinit var documentConversionPort: DocumentConversionPort
    private lateinit var useCase: GenerateResumeExportUseCaseService

    @BeforeEach
    fun setup() {
        resumeRepositoryPort = mockk()
        resumeTemplateRendererPort = mockk()
        plainTextRendererPort = mockk()
        documentConversionPort = mockk()
        useCase = GenerateResumeExportUseCaseService(
            resumeRepositoryPort,
            resumeTemplateRendererPort,
            plainTextRendererPort,
            documentConversionPort
        )
    }

    @Test
    fun `pdf export renders HTML then converts with PDF format`() {
        val resume = sampleResume()
        val query = GenerateResumeExportQuery(resume.id, resume.userId, ResumeTemplateType.MODERN, ResumeExportFormat.PDF)
        val html = "<html>resume</html>"
        val bytes = "pdf".toByteArray()

        every { resumeRepositoryPort.findByIdAndUserId(resume.id, resume.userId) } returns Mono.just(resume)
        every { resumeTemplateRendererPort.render(resume, ResumeTemplateType.MODERN) } returns Mono.just(html)
        every { documentConversionPort.convert(html, ResumeExportFormat.PDF) } returns Mono.just(bytes)

        StepVerifier.create(useCase.handle(query))
            .assertNext { result: ResumeExportResult ->
                assertThat(result.bytes).isEqualTo(bytes)
                assertThat(result.contentType).isEqualTo("application/pdf")
                assertThat(result.fileName).isEqualTo("senior-backend-engineer-modern.pdf")
            }
            .verifyComplete()

        verify(exactly = 1) { documentConversionPort.convert(html, ResumeExportFormat.PDF) }
        verify(exactly = 0) { plainTextRendererPort.render(any()) }
    }

    @Test
    fun `png export converts with PNG format`() {
        val resume = sampleResume()
        val query = GenerateResumeExportQuery(resume.id, resume.userId, ResumeTemplateType.DEFAULT, ResumeExportFormat.PNG)
        val html = "<html>resume</html>"
        val bytes = byteArrayOf(0x89.toByte(), 0x50)

        every { resumeRepositoryPort.findByIdAndUserId(resume.id, resume.userId) } returns Mono.just(resume)
        every { resumeTemplateRendererPort.render(resume, ResumeTemplateType.DEFAULT) } returns Mono.just(html)
        every { documentConversionPort.convert(html, ResumeExportFormat.PNG) } returns Mono.just(bytes)

        StepVerifier.create(useCase.handle(query))
            .assertNext { result ->
                assertThat(result.contentType).isEqualTo("image/png")
                assertThat(result.fileName).isEqualTo("senior-backend-engineer-default.png")
            }
            .verifyComplete()
    }

    @Test
    fun `txt export uses plain-text renderer without HTML or conversion`() {
        val resume = sampleResume()
        val query = GenerateResumeExportQuery(resume.id, resume.userId, ResumeTemplateType.MODERN, ResumeExportFormat.TXT)

        every { resumeRepositoryPort.findByIdAndUserId(resume.id, resume.userId) } returns Mono.just(resume)
        every { plainTextRendererPort.render(resume) } returns "이력서 본문"

        StepVerifier.create(useCase.handle(query))
            .assertNext { result ->
                assertThat(result.contentType).isEqualTo("text/plain;charset=UTF-8")
                // TXT는 템플릿 무관 → 템플릿 접미사 없음
                assertThat(result.fileName).isEqualTo("senior-backend-engineer.txt")
                assertThat(String(result.bytes, Charsets.UTF_8)).isEqualTo("이력서 본문")
            }
            .verifyComplete()

        verify(exactly = 1) { plainTextRendererPort.render(resume) }
        verify(exactly = 0) { resumeTemplateRendererPort.render(any(), any()) }
        verify(exactly = 0) { documentConversionPort.convert(any(), any()) }
    }

    @Test
    fun `handle emits not found when resume missing`() {
        val resumeId = UUID.randomUUID()
        val query = GenerateResumeExportQuery(resumeId, "user-123", ResumeTemplateType.DEFAULT, ResumeExportFormat.PDF)

        every { resumeRepositoryPort.findByIdAndUserId(resumeId, "user-123") } returns Mono.empty()

        StepVerifier.create(useCase.handle(query))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                assertThat((error as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()
    }

    private fun sampleResume(): Resume =
        Resume(
            id = UUID.randomUUID(),
            userId = "user-123",
            resumeData = ResumeData(
                summary = ResumeSummary(
                    headline = "Senior Backend Engineer",
                    profile = listOf("10+ years experience"),
                    coreStrengths = listOf("Kotlin", "Reactive Systems")
                ),
                experience = listOf(
                    Experience(company = "Acme", position = "Engineer", duration = "2020-2024")
                )
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 1,
            isActive = true
        )
}

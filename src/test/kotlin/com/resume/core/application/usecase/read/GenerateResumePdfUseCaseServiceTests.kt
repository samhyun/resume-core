package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GenerateResumePdfQuery
import com.resume.core.application.dto.read.ResumePdfResult
import com.resume.core.domain.model.*
import com.resume.core.port.outbound.external.PdfConversionPort
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
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

class GenerateResumePdfUseCaseServiceTests {

    private lateinit var resumeRepositoryPort: ResumeRepositoryPort
    private lateinit var resumeTemplateRendererPort: ResumeTemplateRendererPort
    private lateinit var pdfConversionPort: PdfConversionPort
    private lateinit var useCase: GenerateResumePdfUseCaseService

    @BeforeEach
    fun setup() {
        resumeRepositoryPort = mockk()
        resumeTemplateRendererPort = mockk()
        pdfConversionPort = mockk()
        useCase = GenerateResumePdfUseCaseService(
            resumeRepositoryPort,
            resumeTemplateRendererPort,
            pdfConversionPort
        )
    }

    @Test
    fun `handle should render HTML and convert it to PDF`() {
        val resume = sampleResume()
        val query = GenerateResumePdfQuery(resume.id, resume.userId, ResumeTemplateType.MODERN)
        val renderedHtml = "<html>resume</html>"
        val pdfBytes = "pdf".toByteArray()

        every { resumeRepositoryPort.findByIdAndUserId(resume.id, resume.userId) } returns Mono.just(resume)
        every { resumeTemplateRendererPort.render(resume, ResumeTemplateType.MODERN) } returns Mono.just(renderedHtml)
        every { pdfConversionPort.convert(renderedHtml) } returns Mono.just(pdfBytes)

        StepVerifier.create(useCase.handle(query))
            .assertNext { result: ResumePdfResult ->
                assertThat(result.bytes).isEqualTo(pdfBytes)
                assertThat(result.contentType).isEqualTo("application/pdf")
                assertThat(result.fileName).contains("modern")
            }
            .verifyComplete()

        verify(exactly = 1) { resumeRepositoryPort.findByIdAndUserId(resume.id, resume.userId) }
        verify(exactly = 1) { resumeTemplateRendererPort.render(resume, ResumeTemplateType.MODERN) }
        verify(exactly = 1) { pdfConversionPort.convert(renderedHtml) }
    }

    @Test
    fun `handle should emit not found when resume missing`() {
        val resumeId = UUID.randomUUID()
        val query = GenerateResumePdfQuery(resumeId, "user-123", ResumeTemplateType.DEFAULT)

        every { resumeRepositoryPort.findByIdAndUserId(resumeId, "user-123") } returns Mono.empty()

        StepVerifier.create(useCase.handle(query))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val statusException = error as ResponseStatusException
                assertThat(statusException.statusCode.value()).isEqualTo(404)
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
                    Experience(
                        company = "Acme",
                        position = "Engineer",
                        duration = "2020-2024",
                        summary = "Built stuff",
                        projects = listOf(
                            Project(
                                name = "Project X",
                                period = "2023",
                                challenge = "Scale",
                                actions = listOf("Led team"),
                                results = listOf("Launched"),
                                technologies = listOf("Kotlin"),
                                links = null
                            )
                        )
                    )
                ),
                skills = Skills(
                    programming = listOf("Kotlin"),
                    frameworks = listOf("Spring"),
                    databases = listOf("Postgres"),
                    tools = listOf("Docker"),
                    cloud = listOf("AWS"),
                    languages = listOf("English")
                ),
                projects = null,
                education = listOf(
                    Education(
                        institution = "Uni",
                        degree = "BS",
                        major = "CS",
                        graduationYear = "2012",
                        gpa = null,
                        achievements = null
                    )
                ),
                certificationsAwards = null,
                additionalInfo = null
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 1,
            isActive = true
        )
}

package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.UpdateResumeCommand
import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeData
import com.resume.core.domain.model.ResumeSummary
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.UUID

class UpdateResumeUseCaseServiceTests {

    private lateinit var resumeRepositoryPort: ResumeRepositoryPort
    private lateinit var useCase: UpdateResumeUseCaseService

    @BeforeEach
    fun setup() {
        resumeRepositoryPort = mockk()
        useCase = UpdateResumeUseCaseService(resumeRepositoryPort)
    }

    @Test
    fun `handle should bump version and persist when the resume is owned`() {
        val resumeId = UUID.randomUUID()
        val existing = sampleResume(resumeId, version = 3)
        val command = UpdateResumeCommand(resumeId = resumeId, userId = "user-1", resumeData = newData())

        val captured = slot<Resume>()
        every { resumeRepositoryPort.findByIdAndUserId(resumeId, "user-1") } returns Mono.just(existing)
        every { resumeRepositoryPort.update(capture(captured)) } answers { Mono.just(firstArg()) }

        StepVerifier.create(useCase.handle(command))
            .assertNext { result ->
                assertThat(result.resumeId).isEqualTo(resumeId)
                assertThat(result.userId).isEqualTo("user-1")
                assertThat(result.version).isEqualTo(4) // 기존 3 → +1
            }
            .verifyComplete()

        val updated = captured.captured
        assertThat(updated.version).isEqualTo(4)
        assertThat(updated.resumeData).isEqualTo(command.resumeData)
        verify(exactly = 1) { resumeRepositoryPort.update(any()) }
    }

    @Test
    fun `handle should emit not found when the resume is missing or not owned`() {
        val resumeId = UUID.randomUUID()
        val command = UpdateResumeCommand(resumeId = resumeId, userId = "user-1", resumeData = newData())

        every { resumeRepositoryPort.findByIdAndUserId(resumeId, "user-1") } returns Mono.empty()

        StepVerifier.create(useCase.handle(command))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                assertThat((error as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()

        verify(exactly = 0) { resumeRepositoryPort.update(any()) }
    }

    private fun newData(): ResumeData =
        ResumeData(summary = ResumeSummary(headline = "Updated headline"))

    private fun sampleResume(id: UUID, version: Int): Resume =
        Resume(
            id = id,
            userId = "user-1",
            resumeData = ResumeData(summary = ResumeSummary(headline = "Original")),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = version,
            isActive = true,
        )
}

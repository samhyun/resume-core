package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.SaveResumeCommand
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
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class SaveResumeUseCaseServiceTests {

    private lateinit var resumeRepositoryPort: ResumeRepositoryPort
    private lateinit var useCase: SaveResumeUseCaseService

    @BeforeEach
    fun setup() {
        resumeRepositoryPort = mockk()
        useCase = SaveResumeUseCaseService(resumeRepositoryPort)
    }

    @Test
    fun `handle should build a new active resume and return its projection`() {
        val command = SaveResumeCommand(userId = "user-1", resumeData = sampleData())

        val captured = slot<Resume>()
        // 저장 어댑터는 넘겨받은 Resume 를 그대로 돌려준다고 가정한다.
        every { resumeRepositoryPort.save(capture(captured)) } answers { Mono.just(firstArg()) }

        StepVerifier.create(useCase.handle(command))
            .assertNext { result ->
                assertThat(result.userId).isEqualTo("user-1")
                assertThat(result.version).isEqualTo(1)
                assertThat(result.resumeId).isEqualTo(captured.captured.id)
            }
            .verifyComplete()

        // 신규 저장은 항상 version=1, isActive=true, 랜덤 UUID 로 생성된다.
        val saved = captured.captured
        assertThat(saved.userId).isEqualTo("user-1")
        assertThat(saved.version).isEqualTo(1)
        assertThat(saved.isActive).isTrue()
        assertThat(saved.resumeData).isEqualTo(command.resumeData)
        verify(exactly = 1) { resumeRepositoryPort.save(any()) }
    }

    private fun sampleData(): ResumeData =
        ResumeData(
            summary = ResumeSummary(
                headline = "Backend Engineer",
                profile = listOf("5 years"),
                coreStrengths = listOf("Kotlin"),
            ),
        )
}

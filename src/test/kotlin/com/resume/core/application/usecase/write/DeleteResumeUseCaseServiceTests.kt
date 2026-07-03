package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.DeleteResumeCommand
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.util.UUID

class DeleteResumeUseCaseServiceTests {

    private lateinit var resumeRepositoryPort: ResumeRepositoryPort
    private lateinit var useCase: DeleteResumeUseCaseService

    @BeforeEach
    fun setup() {
        resumeRepositoryPort = mockk()
        useCase = DeleteResumeUseCaseService(resumeRepositoryPort)
    }

    @Test
    fun `handle should delete resume when it exists`() {
        val resumeId = UUID.randomUUID()
        val command = DeleteResumeCommand(resumeId = resumeId, userId = "user-1")

        every { resumeRepositoryPort.existsByIdAndUserId(resumeId, "user-1") } returns Mono.just(true)
        every { resumeRepositoryPort.deleteByIdAndUserId(resumeId, "user-1") } returns Mono.just(Unit)

        StepVerifier.create(useCase.handle(command))
            .expectNext(Unit)
            .verifyComplete()

        verify(exactly = 1) { resumeRepositoryPort.deleteByIdAndUserId(resumeId, "user-1") }
    }

    @Test
    fun `handle should emit not found when resume missing`() {
        val resumeId = UUID.randomUUID()
        val command = DeleteResumeCommand(resumeId = resumeId, userId = "user-1")

        every { resumeRepositoryPort.existsByIdAndUserId(resumeId, "user-1") } returns Mono.just(false)

        StepVerifier.create(useCase.handle(command))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val statusException = error as ResponseStatusException
                assertThat(statusException.statusCode.value()).isEqualTo(404)
            }
            .verify()
    }
}

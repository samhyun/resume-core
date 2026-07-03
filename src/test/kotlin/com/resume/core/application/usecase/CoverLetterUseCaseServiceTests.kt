package com.resume.core.application.usecase

import com.resume.core.application.dto.read.GetCoverLetterQuery
import com.resume.core.application.dto.read.ListCoverLettersQuery
import com.resume.core.application.dto.write.DeleteCoverLetterCommand
import com.resume.core.application.dto.write.SaveCoverLetterCommand
import com.resume.core.application.dto.write.UpdateCoverLetterCommand
import com.resume.core.application.usecase.read.GetCoverLetterUseCaseService
import com.resume.core.application.usecase.read.ListCoverLettersUseCaseService
import com.resume.core.application.usecase.write.DeleteCoverLetterUseCaseService
import com.resume.core.application.usecase.write.SaveCoverLetterUseCaseService
import com.resume.core.application.usecase.write.UpdateCoverLetterUseCaseService
import com.resume.core.domain.model.CoverLetter
import com.resume.core.port.outbound.persistence.CoverLetterRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.UUID

class CoverLetterUseCaseServiceTests {

    private lateinit var repo: CoverLetterRepositoryPort
    private lateinit var saveService: SaveCoverLetterUseCaseService
    private lateinit var updateService: UpdateCoverLetterUseCaseService
    private lateinit var deleteService: DeleteCoverLetterUseCaseService
    private lateinit var getService: GetCoverLetterUseCaseService
    private lateinit var listService: ListCoverLettersUseCaseService

    @BeforeEach
    fun setup() {
        repo = mockk()
        saveService = SaveCoverLetterUseCaseService(repo)
        updateService = UpdateCoverLetterUseCaseService(repo)
        deleteService = DeleteCoverLetterUseCaseService(repo)
        getService = GetCoverLetterUseCaseService(repo)
        listService = ListCoverLettersUseCaseService(repo)
    }

    @Test
    fun `save creates cover letter with generated id and version 1`() {
        every { repo.save(any()) } answers { Mono.just(firstArg()) }

        val command = SaveCoverLetterCommand(
            userId = "user-1",
            resumeId = UUID.randomUUID(),
            companyName = "Acme",
            position = "Backend Engineer",
            jobDescription = "JD text",
            content = "본문",
            validationScore = 85
        )

        StepVerifier.create(saveService.handle(command))
            .assertNext { saved ->
                assertThat(saved.companyName).isEqualTo("Acme")
                assertThat(saved.content).isEqualTo("본문")
                assertThat(saved.validationScore).isEqualTo(85)
                assertThat(saved.version).isEqualTo(1)
                assertThat(saved.userId).isEqualTo("user-1")
            }
            .verifyComplete()

        verify(exactly = 1) { repo.save(any()) }
    }

    @Test
    fun `update emits not found when cover letter missing`() {
        every { repo.findByIdAndUserId(any(), any()) } returns Mono.empty()

        val command = UpdateCoverLetterCommand(UUID.randomUUID(), "user-1", "Acme", "BE", null, "수정본")

        StepVerifier.create(updateService.handle(command))
            .expectErrorSatisfies { error ->
                assertThat((error as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()
    }

    @Test
    fun `update applies edited fields`() {
        val id = UUID.randomUUID()
        val existing = sampleCoverLetter(id, "user-1").copy(companyName = "Old", content = "old", version = 1)
        every { repo.findByIdAndUserId(id, "user-1") } returns Mono.just(existing)
        every { repo.update(any()) } answers { Mono.just(firstArg<CoverLetter>().copy(version = 2)) }

        val command = UpdateCoverLetterCommand(id, "user-1", "NewCo", "Senior BE", "new JD", "수정된 본문")

        StepVerifier.create(updateService.handle(command))
            .assertNext { updated ->
                assertThat(updated.companyName).isEqualTo("NewCo")
                assertThat(updated.position).isEqualTo("Senior BE")
                assertThat(updated.content).isEqualTo("수정된 본문")
                assertThat(updated.version).isEqualTo(2)
            }
            .verifyComplete()
    }

    @Test
    fun `delete emits not found when cover letter does not exist`() {
        every { repo.existsByIdAndUserId(any(), any()) } returns Mono.just(false)

        StepVerifier.create(deleteService.handle(DeleteCoverLetterCommand(UUID.randomUUID(), "user-1")))
            .expectErrorSatisfies { error ->
                assertThat((error as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()
    }

    @Test
    fun `delete removes when owned`() {
        val id = UUID.randomUUID()
        every { repo.existsByIdAndUserId(id, "user-1") } returns Mono.just(true)
        every { repo.deleteByIdAndUserId(id, "user-1") } returns Mono.just(Unit)

        StepVerifier.create(deleteService.handle(DeleteCoverLetterCommand(id, "user-1")))
            .expectNext(Unit)
            .verifyComplete()

        verify(exactly = 1) { repo.deleteByIdAndUserId(id, "user-1") }
    }

    @Test
    fun `get and list delegate to repository`() {
        val id = UUID.randomUUID()
        val cl = sampleCoverLetter(id, "user-1")
        every { repo.findByIdAndUserId(id, "user-1") } returns Mono.just(cl)
        every { repo.findAllByUserId("user-1") } returns Flux.just(cl)

        StepVerifier.create(getService.handle(GetCoverLetterQuery(id, "user-1")))
            .expectNext(cl)
            .verifyComplete()

        StepVerifier.create(listService.handle(ListCoverLettersQuery("user-1")))
            .expectNext(cl)
            .verifyComplete()
    }

    private fun sampleCoverLetter(id: UUID, userId: String): CoverLetter {
        val now = LocalDateTime.now()
        return CoverLetter(
            id = id,
            userId = userId,
            resumeId = null,
            companyName = "Acme",
            position = "BE",
            jobDescription = null,
            content = "본문",
            validationScore = 80,
            version = 1,
            createdAt = now,
            updatedAt = now
        )
    }
}

package com.resume.core.application.usecase

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.application.usecase.read.GetUserProfileUseCaseService
import com.resume.core.application.usecase.write.UpdateUserProfileUseCaseService
import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.persistence.UserProfileRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ProfileUseCaseServiceTests {

    private lateinit var repo: UserProfileRepositoryPort
    private lateinit var getService: GetUserProfileUseCaseService
    private lateinit var updateService: UpdateUserProfileUseCaseService

    @BeforeEach
    fun setup() {
        repo = mockk()
        getService = GetUserProfileUseCaseService(repo)
        updateService = UpdateUserProfileUseCaseService(repo)
    }

    @Test
    fun `get returns the profile`() {
        every { repo.findByUsername("tester") } returns Mono.just(sampleProfile())

        StepVerifier.create(getService.handle("tester"))
            .assertNext { assertThat(it.username).isEqualTo("tester") }
            .verifyComplete()
    }

    @Test
    fun `get emits 404 when profile missing`() {
        every { repo.findByUsername(any()) } returns Mono.empty()

        StepVerifier.create(getService.handle("ghost"))
            .expectErrorSatisfies {
                assertThat((it as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()
    }

    @Test
    fun `update forwards editable fields and returns updated profile`() {
        every { repo.updateProfile("tester", "새 표시명", "길동", "홍") } returns
            Mono.just(sampleProfile().copy(displayName = "새 표시명", firstName = "길동", lastName = "홍"))

        StepVerifier.create(
            updateService.handle(UpdateUserProfileCommand("tester", "새 표시명", "길동", "홍"))
        )
            .assertNext { assertThat(it.displayName).isEqualTo("새 표시명") }
            .verifyComplete()

        verify(exactly = 1) { repo.updateProfile("tester", "새 표시명", "길동", "홍") }
    }

    @Test
    fun `update emits 404 when profile missing`() {
        every { repo.updateProfile(any(), any(), any(), any()) } returns Mono.empty()

        StepVerifier.create(
            updateService.handle(UpdateUserProfileCommand("ghost", "x", null, null))
        )
            .expectErrorSatisfies {
                assertThat((it as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()
    }

    private fun sampleProfile(): UserProfile =
        UserProfile(
            username = "tester",
            email = "tester@example.com",
            displayName = "old",
            firstName = null,
            lastName = null,
            emailVerified = true
        )
}

package com.resume.core.application.usecase

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.application.usecase.read.GetUserProfileUseCaseService
import com.resume.core.application.usecase.write.UpdateUserProfileUseCaseService
import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.external.UserProfilePort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ProfileUseCaseServiceTests {

    private lateinit var port: UserProfilePort
    private lateinit var getService: GetUserProfileUseCaseService
    private lateinit var updateService: UpdateUserProfileUseCaseService

    @BeforeEach
    fun setup() {
        port = mockk()
        getService = GetUserProfileUseCaseService(port)
        updateService = UpdateUserProfileUseCaseService(port)
    }

    @Test
    fun `get forwards the token to the port`() {
        every { port.getProfile("tok") } returns Mono.just(sampleProfile())

        StepVerifier.create(getService.handle("tok"))
            .assertNext { assertThat(it.username).isEqualTo("tester") }
            .verifyComplete()

        verify(exactly = 1) { port.getProfile("tok") }
    }

    @Test
    fun `update forwards token and name fields to the port`() {
        every { port.updateProfile("tok", "길동", "홍") } returns
            Mono.just(sampleProfile().copy(firstName = "길동", lastName = "홍"))

        StepVerifier.create(updateService.handle("tok", UpdateUserProfileCommand("길동", "홍")))
            .assertNext { assertThat(it.firstName).isEqualTo("길동") }
            .verifyComplete()

        verify(exactly = 1) { port.updateProfile("tok", "길동", "홍") }
    }

    private fun sampleProfile(): UserProfile =
        UserProfile(
            username = "tester",
            email = "tester@example.com",
            firstName = null,
            lastName = null,
            emailVerified = true
        )
}

package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.application.usecase.read.GetUserProfileUseCase
import com.resume.core.application.usecase.write.UpdateUserProfileUseCase
import com.resume.core.domain.model.UserProfile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(controllers = [ProfileController::class])
@ContextConfiguration(classes = [ProfileController::class, ProfileControllerTests.TestSecurityConfig::class])
class ProfileControllerTests {

    @Configuration
    @EnableWebFluxSecurity
    class TestSecurityConfig {
        @Bean
        fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
            http
                .csrf { it.disable() }
                .authorizeExchange { it.anyExchange().permitAll() }
                .build()
    }

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean lateinit var getUserProfileUseCase: GetUserProfileUseCase
    @MockitoBean lateinit var updateUserProfileUseCase: UpdateUserProfileUseCase
    @MockitoBean lateinit var authenticationFacade: ReactiveJwtAuthenticationFacade

    @Test
    fun `get returns the current user's profile`() {
        given(authenticationFacade.currentUsername()).willReturn(Mono.just("tester"))
        given(getUserProfileUseCase.handle("tester")).willReturn(Mono.just(sampleProfile()))

        webTestClient.get()
            .uri("/api/resume-core/profile")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.username").isEqualTo("tester")
            .jsonPath("$.email").isEqualTo("tester@example.com")
    }

    @Test
    fun `put updates profile scoped to the current user`() {
        given(authenticationFacade.currentUsername()).willReturn(Mono.just("tester"))
        given(updateUserProfileUseCase.handle(any())).willReturn(
            Mono.just(sampleProfile().copy(displayName = "새 표시명", firstName = "길동"))
        )

        val request = mapOf("displayName" to "새 표시명", "firstName" to "길동")

        webTestClient.put()
            .uri("/api/resume-core/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.displayName").isEqualTo("새 표시명")

        val captor = argumentCaptor<UpdateUserProfileCommand>()
        verify(updateUserProfileUseCase).handle(captor.capture())
        // 항상 JWT 의 preferred_username 으로 스코프 — 타인 프로필 수정 불가
        assertThat(captor.firstValue.username).isEqualTo("tester")
        assertThat(captor.firstValue.displayName).isEqualTo("새 표시명")
        assertThat(captor.firstValue.firstName).isEqualTo("길동")
    }

    @Test
    fun `put rejects an over-long field with 400`() {
        given(authenticationFacade.currentUsername()).willReturn(Mono.just("tester"))

        val request = mapOf("displayName" to "a".repeat(101))

        webTestClient.put()
            .uri("/api/resume-core/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isBadRequest
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

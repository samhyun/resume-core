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
import org.mockito.kotlin.eq
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
        given(authenticationFacade.currentToken()).willReturn(Mono.just("tok"))
        given(getUserProfileUseCase.handle("tok")).willReturn(Mono.just(sampleProfile()))

        webTestClient.get()
            .uri("/api/resume-core/profile")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.username").isEqualTo("tester")
            .jsonPath("$.email").isEqualTo("tester@example.com")
    }

    @Test
    fun `put forwards the token and edited fields`() {
        given(authenticationFacade.currentToken()).willReturn(Mono.just("tok"))
        given(updateUserProfileUseCase.handle(any(), any())).willReturn(
            Mono.just(sampleProfile().copy(firstName = "길동", lastName = "홍"))
        )

        val request = mapOf("firstName" to "길동", "lastName" to "홍")

        webTestClient.put()
            .uri("/api/resume-core/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.firstName").isEqualTo("길동")

        val captor = argumentCaptor<UpdateUserProfileCommand>()
        verify(updateUserProfileUseCase).handle(eq("tok"), captor.capture())
        assertThat(captor.firstValue.firstName).isEqualTo("길동")
        assertThat(captor.firstValue.lastName).isEqualTo("홍")
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

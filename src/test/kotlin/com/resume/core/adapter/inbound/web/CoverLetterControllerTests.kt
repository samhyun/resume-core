package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.application.dto.write.SaveCoverLetterCommand
import com.resume.core.application.usecase.read.GetCoverLetterUseCase
import com.resume.core.application.usecase.read.ListCoverLettersUseCase
import com.resume.core.application.usecase.write.DeleteCoverLetterUseCase
import com.resume.core.application.usecase.write.SaveCoverLetterUseCase
import com.resume.core.application.usecase.write.UpdateCoverLetterUseCase
import com.resume.core.domain.model.CoverLetter
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@WebFluxTest(controllers = [CoverLetterController::class])
@ContextConfiguration(classes = [CoverLetterController::class, CoverLetterControllerTests.TestSecurityConfig::class])
class CoverLetterControllerTests {

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

    @MockitoBean lateinit var saveCoverLetterUseCase: SaveCoverLetterUseCase
    @MockitoBean lateinit var updateCoverLetterUseCase: UpdateCoverLetterUseCase
    @MockitoBean lateinit var getCoverLetterUseCase: GetCoverLetterUseCase
    @MockitoBean lateinit var listCoverLettersUseCase: ListCoverLettersUseCase
    @MockitoBean lateinit var deleteCoverLetterUseCase: DeleteCoverLetterUseCase
    @MockitoBean lateinit var authenticationFacade: ReactiveJwtAuthenticationFacade

    @Test
    fun `create returns 201 and forwards fields to command`() {
        given(authenticationFacade.currentUserId()).willReturn(Mono.just("user-1"))
        given(saveCoverLetterUseCase.handle(any())).willReturn(Mono.just(sample(UUID.randomUUID())))

        val request = mapOf(
            "companyName" to "Acme",
            "position" to "Backend Engineer",
            "content" to "본문",
            "validationScore" to 85
        )

        webTestClient.post()
            .uri("/api/resume-core/cover-letters")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.companyName").isEqualTo("Acme")

        val captor = argumentCaptor<SaveCoverLetterCommand>()
        verify(saveCoverLetterUseCase).handle(captor.capture())
        assertThat(captor.firstValue.userId).isEqualTo("user-1")
        assertThat(captor.firstValue.content).isEqualTo("본문")
        assertThat(captor.firstValue.validationScore).isEqualTo(85)
    }

    @Test
    fun `list returns the user's cover letters`() {
        given(authenticationFacade.currentUserId()).willReturn(Mono.just("user-1"))
        given(listCoverLettersUseCase.handle(any())).willReturn(Flux.just(sample(UUID.randomUUID())))

        webTestClient.get()
            .uri("/api/resume-core/cover-letters")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].companyName").isEqualTo("Acme")
    }

    @Test
    fun `get returns 404 when not found`() {
        given(authenticationFacade.currentUserId()).willReturn(Mono.just("user-1"))
        given(getCoverLetterUseCase.handle(any())).willReturn(Mono.empty())

        webTestClient.get()
            .uri("/api/resume-core/cover-letters/${UUID.randomUUID()}")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `delete returns 204`() {
        given(authenticationFacade.currentUserId()).willReturn(Mono.just("user-1"))
        given(deleteCoverLetterUseCase.handle(any())).willReturn(Mono.just(Unit))

        webTestClient.delete()
            .uri("/api/resume-core/cover-letters/${UUID.randomUUID()}")
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    fun `invalid id returns 400`() {
        webTestClient.get()
            .uri("/api/resume-core/cover-letters/not-a-uuid")
            .exchange()
            .expectStatus().isBadRequest
    }

    private fun sample(id: UUID): CoverLetter {
        val now = LocalDateTime.now()
        return CoverLetter(
            id = id,
            userId = "user-1",
            resumeId = null,
            companyName = "Acme",
            position = "Backend Engineer",
            jobDescription = null,
            content = "본문",
            validationScore = 85,
            version = 1,
            createdAt = now,
            updatedAt = now
        )
    }
}

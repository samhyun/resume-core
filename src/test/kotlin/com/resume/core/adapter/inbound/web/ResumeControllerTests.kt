package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.application.dto.read.GenerateResumeExportQuery
import com.resume.core.application.dto.read.ResumeExportResult
import com.resume.core.application.usecase.read.GenerateResumeExportUseCase
import com.resume.core.application.usecase.read.GetActiveResumeUseCase
import com.resume.core.application.usecase.read.GetResumeUseCase
import com.resume.core.application.usecase.read.ListResumesUseCase
import com.resume.core.application.usecase.write.DeleteResumeUseCase
import com.resume.core.application.usecase.write.SaveResumeUseCase
import com.resume.core.application.usecase.write.UpdateResumeUseCase
import com.resume.core.domain.model.ResumeExportFormat
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
import java.util.UUID

@WebFluxTest(controllers = [ResumeController::class])
@ContextConfiguration(classes = [ResumeController::class, ResumeControllerTests.TestSecurityConfig::class])
class ResumeControllerTests {

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

    @MockitoBean lateinit var saveResumeUseCase: SaveResumeUseCase
    @MockitoBean lateinit var updateResumeUseCase: UpdateResumeUseCase
    @MockitoBean lateinit var getResumeUseCase: GetResumeUseCase
    @MockitoBean lateinit var getActiveResumeUseCase: GetActiveResumeUseCase
    @MockitoBean lateinit var listResumesUseCase: ListResumesUseCase
    @MockitoBean lateinit var deleteResumeUseCase: DeleteResumeUseCase
    @MockitoBean lateinit var generateResumeExportUseCase: GenerateResumeExportUseCase
    @MockitoBean lateinit var authenticationFacade: ReactiveJwtAuthenticationFacade

    @Test
    fun `export txt returns text-plain attachment and passes TXT format`() {
        given(authenticationFacade.currentUserId()).willReturn(Mono.just("user-1"))
        given(generateResumeExportUseCase.handle(any())).willReturn(
            Mono.just(ResumeExportResult("hong.txt", "text/plain;charset=UTF-8", "본문".toByteArray()))
        )

        val resumeId = UUID.randomUUID()
        webTestClient.get()
            .uri("/api/resume-core/resumes/$resumeId/export?format=txt")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
            .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"hong.txt\"")

        val captor = argumentCaptor<GenerateResumeExportQuery>()
        verify(generateResumeExportUseCase).handle(captor.capture())
        assertThat(captor.firstValue.format).isEqualTo(ResumeExportFormat.TXT)
    }

    @Test
    fun `export rejects unknown format with 400`() {
        val resumeId = UUID.randomUUID()
        webTestClient.get()
            .uri("/api/resume-core/resumes/$resumeId/export?format=docx")
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `pdf endpoint stays backward compatible and delegates with PDF format`() {
        given(authenticationFacade.currentUserId()).willReturn(Mono.just("user-1"))
        given(generateResumeExportUseCase.handle(any())).willReturn(
            Mono.just(ResumeExportResult("hong-default.pdf", "application/pdf", byteArrayOf(1, 2)))
        )

        val resumeId = UUID.randomUUID()
        webTestClient.get()
            .uri("/api/resume-core/resumes/$resumeId/pdf")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentType(MediaType.APPLICATION_PDF)

        val captor = argumentCaptor<GenerateResumeExportQuery>()
        verify(generateResumeExportUseCase).handle(captor.capture())
        assertThat(captor.firstValue.format).isEqualTo(ResumeExportFormat.PDF)
    }
}

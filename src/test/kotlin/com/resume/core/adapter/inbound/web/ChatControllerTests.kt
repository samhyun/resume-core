package com.resume.core.adapter.inbound.web

import com.resume.core.application.dto.write.ChatMessagePayload
import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.application.usecase.read.StreamChatSessionUseCase
import com.resume.core.application.usecase.write.CreateChatSessionUseCase
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.UUID

@WebFluxTest(
    controllers = [ChatController::class],
    excludeAutoConfiguration = [
        ReactiveOAuth2ClientAutoConfiguration::class,
        ReactiveOAuth2ResourceServerAutoConfiguration::class
    ]
)
@ContextConfiguration(classes = [ChatController::class, ChatControllerTests.TestSecurityConfig::class])
class ChatControllerTests {

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

    @MockitoBean
    lateinit var createChatSessionUseCase: CreateChatSessionUseCase

    @MockitoBean
    lateinit var streamChatSessionUseCase: StreamChatSessionUseCase

    @Test
    fun `runSseMultipart accepts file and streams events`() {
        val events = Flux.just(
            AiAgentStreamEvent(id = "evt-1", data = "{\"text\":\"Hello\"}"),
            AiAgentStreamEvent(event = "end")
        )
        given(streamChatSessionUseCase.stream(any())).willReturn(events)

        val sessionId = UUID.randomUUID()
        val builder = MultipartBodyBuilder()
        builder.part("sessionId", sessionId.toString())
        builder.part(
            "file",
            NamedByteArrayResource("test".toByteArray(), filename = "resume.pdf")
        ).contentType(MediaType.APPLICATION_PDF)
        builder.part("displayName", "resume.pdf")

        val result = webTestClient.post()
            .uri("/api/resume-core/chats/run-sse")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})

        val responseEvents = result.responseBody.collectList().block(Duration.ofSeconds(1))
        assertThat(responseEvents).isNotNull
        assertThat(responseEvents!!).hasSize(2)
        assertThat(responseEvents[0].id()).isEqualTo("evt-1")
        assertThat(responseEvents[0].data()).isEqualTo("{\"text\":\"Hello\"}")
        assertThat(responseEvents[1].event()).isEqualTo("end")

        val captor = argumentCaptor<RunChatSessionCommand>()
        verify(streamChatSessionUseCase).stream(captor.capture())
        val captured = captor.firstValue
        assertThat(captured.sessionId).isEqualTo(sessionId)
        val payload = captured.message as ChatMessagePayload.File
        assertThat(payload.displayName).isEqualTo("resume.pdf")
        assertThat(payload.mimeType).isEqualTo("application/pdf")
        assertThat(payload.part.filename()).isEqualTo("resume.pdf")
    }

    @Test
    fun `runSseJson accepts json text message`() {
        val events = Flux.just(
            AiAgentStreamEvent(id = "evt-1", data = "{\"text\":\"JSON response\"}"),
            AiAgentStreamEvent(event = "end")
        )
        given(streamChatSessionUseCase.stream(any())).willReturn(events)

        val sessionId = UUID.randomUUID()
        val request = mapOf(
            "sessionId" to sessionId.toString(),
            "text" to "  JSON 메시지입니다  "
        )

        val result = webTestClient.post()
            .uri("/api/resume-core/chats/run-sse")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .returnResult(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})

        val responseEvents = result.responseBody.collectList().block(Duration.ofSeconds(1))
        assertThat(responseEvents).isNotNull
        assertThat(responseEvents!!).hasSize(2)
        assertThat(responseEvents[0].id()).isEqualTo("evt-1")
        assertThat(responseEvents[0].data()).isEqualTo("{\"text\":\"JSON response\"}")

        val captor = argumentCaptor<RunChatSessionCommand>()
        verify(streamChatSessionUseCase).stream(captor.capture())
        val captured = captor.firstValue
        assertThat(captured.sessionId).isEqualTo(sessionId)
        val payload = captured.message as ChatMessagePayload.Text
        assertThat(payload.text).isEqualTo("JSON 메시지입니다")
    }

    private class NamedByteArrayResource(
        private val bytes: ByteArray,
        private val filename: String
    ) : ByteArrayResource(bytes) {
        override fun getFilename(): String = filename
    }
}

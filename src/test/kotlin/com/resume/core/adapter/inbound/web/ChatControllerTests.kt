package com.resume.core.adapter.inbound.web

import com.resume.core.application.dto.write.ChatMessagePayload
import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.application.usecase.read.StreamChatSessionUseCase
import com.resume.core.application.usecase.write.CreateChatSessionUseCase
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.UUID

@WebFluxTest(ChatController::class)
class ChatControllerTests {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockBean
    lateinit var createChatSessionUseCase: CreateChatSessionUseCase

    @MockBean
    lateinit var streamChatSessionUseCase: StreamChatSessionUseCase

    @Test
    fun `runSse accepts multipart file and streams events`() {
        val events = Flux.just(
            AiAgentStreamEvent(id = "evt-1", data = "{\"text\":\"Hello\"}"),
            AiAgentStreamEvent(event = "end")
        )
        given(streamChatSessionUseCase.stream(ArgumentMatchers.any(RunChatSessionCommand::class.java))).willReturn(events)

        val sessionId = UUID.randomUUID()
        val builder = MultipartBodyBuilder()
        builder.part("sessionId", sessionId.toString())
        builder.part(
            "file",
            NamedByteArrayResource("test".toByteArray(), filename = "resume.pdf")
        ).contentType(MediaType.APPLICATION_PDF)
        builder.part("displayName", "resume.pdf")

        val result = webTestClient.post()
            .uri("/api/chats/run-sse")
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

        val captor = ArgumentCaptor.forClass(RunChatSessionCommand::class.java)
        verify(streamChatSessionUseCase).stream(captor.capture())
        val captured = captor.value
        assertThat(captured.sessionId).isEqualTo(sessionId)
        val payload = captured.message as ChatMessagePayload.File
        assertThat(payload.displayName).isEqualTo("resume.pdf")
        assertThat(payload.mimeType).isEqualTo("application/pdf")
        assertThat(payload.part.filename()).isEqualTo("resume.pdf")
    }

    @Test
    fun `runSse accepts text message`() {
        val events = Flux.just(AiAgentStreamEvent(data = "{\"text\":\"Hello\"}"))
        given(streamChatSessionUseCase.stream(ArgumentMatchers.any(RunChatSessionCommand::class.java))).willReturn(events)

        val sessionId = UUID.randomUUID()
        val builder = MultipartBodyBuilder()
        builder.part("sessionId", sessionId.toString())
        builder.part("text", "  안녕하세요  ")

        webTestClient.post()
            .uri("/api/chats/run-sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk

        val captor = ArgumentCaptor.forClass(RunChatSessionCommand::class.java)
        verify(streamChatSessionUseCase).stream(captor.capture())
        val captured = captor.value
        assertThat(captured.sessionId).isEqualTo(sessionId)
        val payload = captured.message as ChatMessagePayload.Text
        assertThat(payload.text).isEqualTo("안녕하세요")
    }

    private class NamedByteArrayResource(
        private val bytes: ByteArray,
        private val filename: String
    ) : ByteArrayResource(bytes) {
        override fun getFilename(): String = filename
    }
}

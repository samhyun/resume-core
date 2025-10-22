package com.resume.core.application.usecase.read

import com.resume.core.adapter.outbound.persistence.command.entity.ChatSessionEntity
import com.resume.core.adapter.outbound.persistence.command.repository.ChatSessionRepository
import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.r2dbc.postgresql.codec.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.util.Base64
import java.util.UUID

class StreamChatSessionUseCaseServiceTest {

    private lateinit var agent: AiAgentPort
    private lateinit var repository: ChatSessionRepository
    private lateinit var useCase: StreamChatSessionUseCaseService

    private val factory = DefaultDataBufferFactory()

    @BeforeEach
    fun setup() {
        agent = mockk()
        repository = mockk()
        useCase = StreamChatSessionUseCaseService(agent, repository)
    }

    @Test
    fun `stream delegates to agent for text messages`() {
        val sessionId = UUID.randomUUID()
        val entity = sampleEntity(sessionId)

        val captured = slot<com.resume.core.application.dto.write.RunAgentSessionCommand>()
        every { repository.findById(sessionId) } returns Mono.just(entity)
        every { agent.runSession(capture(captured)) } returns Flux.just(AiAgentStreamEvent(data = "ok"))

        StepVerifier.create(useCase.stream(RunChatSessionCommand.text(sessionId, "Hello")))
            .expectNext(AiAgentStreamEvent(data = "ok"))
            .verifyComplete()

        val command = captured.captured
        assertThat(command.appName).isEqualTo(entity.appName)
        assertThat(command.userId).isEqualTo(entity.userId)
        assertThat(command.sessionId).isEqualTo(entity.agentSessionId)

        val agentMessage = command.newMessage
        assertThat(agentMessage.role).isEqualTo("user")
        assertThat(agentMessage.parts).hasSize(1)
        assertThat(agentMessage.parts[0].text).isEqualTo("Hello")
        assertThat(agentMessage.parts[0].inlineData).isNull()
    }

    @Test
    fun `stream encodes file payloads as inline data`() {
        val sessionId = UUID.randomUUID()
        val entity = sampleEntity(sessionId)
        val fileBytes = "resume".toByteArray()
        val filePart = stubFilePart(
            filename = "resume.pdf",
            mediaType = MediaType.APPLICATION_PDF,
            payloads = listOf(fileBytes)
        )

        val captured = slot<com.resume.core.application.dto.write.RunAgentSessionCommand>()
        every { repository.findById(sessionId) } returns Mono.just(entity)
        every { agent.runSession(capture(captured)) } returns Flux.just(AiAgentStreamEvent(event = "delta"))

        StepVerifier.create(
            useCase.stream(
                RunChatSessionCommand.file(
                    sessionId = sessionId,
                    displayName = "resume.pdf",
                    mimeType = MediaType.APPLICATION_PDF_VALUE,
                    part = filePart
                )
            )
        )
            .expectNext(AiAgentStreamEvent(event = "delta"))
            .verifyComplete()

        val command = captured.captured
        val inlineData = command.newMessage.parts[0].inlineData
        assertThat(inlineData).isNotNull
        assertThat(inlineData!!.displayName).isEqualTo("resume.pdf")
        assertThat(inlineData.mimeType).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        assertThat(inlineData.data).isEqualTo(Base64.getEncoder().encodeToString(fileBytes))
    }

    @Test
    fun `stream fails when chat session does not exist`() {
        val sessionId = UUID.randomUUID()
        every { repository.findById(sessionId) } returns Mono.empty<ChatSessionEntity>()

        StepVerifier.create(useCase.stream(RunChatSessionCommand.text(sessionId, "Hi")))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode.value()).isEqualTo(404)
                assertThat(ex.reason).contains("not found")
            }
            .verify()

        verify(exactly = 0) { agent.runSession(any()) }
    }

    @Test
    fun `stream rejects empty file payload`() {
        val sessionId = UUID.randomUUID()
        val entity = sampleEntity(sessionId)
        val filePart = stubFilePart(
            filename = "empty.pdf",
            mediaType = MediaType.APPLICATION_PDF,
            payloads = listOf(ByteArray(0))
        )

        every { repository.findById(sessionId) } returns Mono.just(entity)

        StepVerifier.create(
            useCase.stream(
                RunChatSessionCommand.file(
                    sessionId = sessionId,
                    displayName = "empty.pdf",
                    mimeType = MediaType.APPLICATION_PDF_VALUE,
                    part = filePart
                )
            )
        )
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                val ex = error as ResponseStatusException
                assertThat(ex.statusCode.value()).isEqualTo(400)
                assertThat(ex.reason).contains("File data must not be empty")
            }
            .verify()

        verify(exactly = 0) { agent.runSession(any()) }
    }

    private fun sampleEntity(sessionId: UUID): ChatSessionEntity =
        ChatSessionEntity(
            id = sessionId,
            userId = "user-1",
            agentSessionId = "agent-123",
            appName = "resume-agent",
            state = Json.of("{}"),
            purpose = "general",
            status = "ACTIVE",
            lastUpdateTime = 42.0,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            endedAt = null
        )

    private fun stubFilePart(
        filename: String,
        mediaType: MediaType,
        payloads: List<ByteArray>
    ): FilePart {
        val headers = HttpHeaders().apply { contentType = mediaType }
        return object : FilePart {
            override fun filename(): String = filename

            override fun headers(): HttpHeaders = headers

            override fun name(): String = "file"

            override fun content(): Flux<DataBuffer> = Flux.defer {
                Flux.fromIterable(payloads.map { bytes -> factory.wrap(bytes.copyOf()) })
            }

            override fun transferTo(dest: File): Mono<Void> = Mono.empty()

            override fun transferTo(dest: Path): Mono<Void> = Mono.empty()
        }
    }
}

package com.resume.core.adapter.inbound.web

import com.resume.core.application.dto.write.CreateChatSessionResult
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.application.dto.write.SessionIds
import com.resume.core.application.dto.write.SessionPurpose
import com.resume.core.application.usecase.read.StreamChatSessionUseCase
import com.resume.core.application.usecase.write.CreateChatSessionUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

@RestController
@RequestMapping("/api/resume-core/chats")
class ChatController(
    private val createChatSessionUseCase: CreateChatSessionUseCase,
    private val streamChatSessionUseCase: StreamChatSessionUseCase
) {

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateChatSessionRequest): Mono<CreateChatSessionResponse> =
        createChatSessionUseCase
            .handle(request.toCommand())
            .map(CreateChatSessionResponse::from)

    @PostMapping(
        "/run-sse",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun runSseMultipart(
        @RequestPart("sessionId") sessionIdValue: String,
        @RequestPart("file") file: FilePart,
        @RequestPart("displayName", required = false) displayName: String?
    ): Flux<ServerSentEvent<String>> {
        val sessionId = sessionIdValue.toUuidOrBadRequest()
        val command = buildFileCommand(sessionId, file, displayName)

        return callChatSessionUseCase(command)
    }

    private fun callChatSessionUseCase(command: RunChatSessionCommand): Flux<ServerSentEvent<String>> = streamChatSessionUseCase
        .stream(command)
        .map { event ->
            val builder = ServerSentEvent.builder<String>()
            event.id?.let(builder::id)
            event.event?.let(builder::event)
            event.retry?.let { builder.retry(Duration.ofMillis(it)) }
            event.comment?.let(builder::comment)
            event.data?.let(builder::data)
            builder.build()
        }

    @PostMapping(
        "/run-sse",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun runSseJson(
        @RequestBody request: RunChatSessionRequest
    ): Flux<ServerSentEvent<String>> {
        val sessionId = request.sessionId.toUuidOrBadRequest()
        val normalized = request.text.trim()

        if (normalized.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Text message must not be blank"
            )
        }

        val command = RunChatSessionCommand.text(sessionId, normalized)

        return callChatSessionUseCase(command)
    }

    private fun buildFileCommand(
        sessionId: UUID,
        file: FilePart,
        displayName: String?
    ): RunChatSessionCommand {
        val resolvedMimeType = file.headers().contentType?.toString()?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "File content type is required"
            )

        val resolvedDisplayName = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: file.filename()

        return RunChatSessionCommand.file(
            sessionId = sessionId,
            displayName = resolvedDisplayName,
            mimeType = resolvedMimeType,
            part = file
        )
    }

    private fun String.toUuidOrBadRequest(): UUID {
        val trimmed = trim()
        if (trimmed.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "sessionId must not be blank"
            )
        }
        return try {
            UUID.fromString(trimmed)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid sessionId format"
            )
        }
    }
}

data class CreateChatSessionRequest(
    val appName: String,
    val userId: String,
    val purpose: SessionPurpose? = null
) {
    fun toCommand(): CreateSessionCommand =
        CreateSessionCommand(
            ids = SessionIds(
                appName = appName,
                userId = userId,
                sessionId = generateSessionId()
            ),
            purpose = purpose ?: SessionPurpose.GENERAL
        )
}

data class CreateChatSessionResponse(
    val sessionId: UUID,
    val agentSessionId: String,
    val appName: String,
    val userId: String,
    val purpose: String?,
    val status: String
) {
    companion object {
        fun from(result: CreateChatSessionResult): CreateChatSessionResponse =
            CreateChatSessionResponse(
                sessionId = result.sessionId,
                agentSessionId = result.agentSessionId,
                appName = result.appName,
                userId = result.userId,
                purpose = result.purpose,
                status = result.status
            )
    }
}

data class RunChatSessionRequest(
    val sessionId: String,
    val text: String
)

private fun generateSessionId(): String = "session-${UUID.randomUUID()}"

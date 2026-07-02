package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.adapter.inbound.web.support.toUuidOrBadRequest
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
    private val streamChatSessionUseCase: StreamChatSessionUseCase,
    private val authenticationFacade: ReactiveJwtAuthenticationFacade,
) {

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateChatSessionRequest): Mono<CreateChatSessionResponse> =
        authenticationFacade.currentUserId()
            .flatMap { userId -> createChatSessionUseCase.handle(request.toCommand(userId)) }
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
        val sessionId = sessionIdValue.toUuidOrBadRequest("sessionId")

        return authenticationFacade.currentUserId()
            .flatMapMany { userId ->
                callChatSessionUseCase(buildFileCommand(sessionId, userId, file, displayName))
            }
    }

    @PostMapping(
        "/run-sse",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.TEXT_EVENT_STREAM_VALUE]
    )
    fun runSseJson(
        @RequestBody request: RunChatSessionRequest
    ): Flux<ServerSentEvent<String>> {
        val sessionId = request.sessionId.toUuidOrBadRequest("sessionId")

        return authenticationFacade.currentUserId()
            .flatMapMany { userId -> callChatSessionUseCase(request.toCommand(sessionId, userId)) }
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

    private fun buildFileCommand(
        sessionId: UUID,
        userId: String,
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
            userId = userId,
            displayName = resolvedDisplayName,
            mimeType = resolvedMimeType,
            part = file
        )
    }
}

data class CreateChatSessionRequest(
    val appName: String,
    val purpose: SessionPurpose? = null,
    // cover_letter / interview 세션 생성 시 이력서 JSON 문자열을 세션 state(resume_data)로 주입. resume_upgrade는 불필요.
    val resumeData: String? = null
) {
    // userId는 요청 본문이 아니라 인증된 JWT(sub)에서만 받는다 — 클라이언트가 타 사용자를 사칭하지 못하게 한다.
    fun toCommand(userId: String): CreateSessionCommand {
        // resumeData 는 선택(미주입=null)이지만, 명시적으로 빈 문자열을 보내면 클라이언트 버그로 보고 거부한다.
        // (비JSON/크기 검증은 하지 않음 — core 는 범용 프록시이고 요청 크기는 WebFlux 코덱이 제한한다.)
        if (resumeData != null && resumeData.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "resumeData must not be blank when provided"
            )
        }
        return CreateSessionCommand(
            ids = SessionIds(
                appName = appName,
                userId = userId,
                sessionId = generateSessionId()
            ),
            purpose = purpose ?: SessionPurpose.GENERAL,
            resumeData = resumeData
        )
    }
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

/**
 * run-sse(JSON) 요청. 일반 메시지는 `text`, HITL 인터럽트 답변은 `functionResponse` 를 보낸다(둘 중 하나만).
 */
data class RunChatSessionRequest(
    val sessionId: String,
    val text: String? = null,
    val functionResponse: FunctionResponseInput? = null
) {
    fun toCommand(sessionId: UUID, userId: String): RunChatSessionCommand {
        functionResponse?.let { fr ->
            // text 와 functionResponse 를 동시에 보내면 클라이언트 오류일 가능성이 높으므로 명시적으로 거부한다.
            if (!text.isNullOrBlank()) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide either text or functionResponse, not both"
                )
            }
            if (fr.id.isBlank() || fr.name.isBlank() || fr.result.isBlank()) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "functionResponse.id, name, result must not be blank"
                )
            }
            return RunChatSessionCommand.functionResponse(
                sessionId = sessionId,
                userId = userId,
                id = fr.id.trim(),
                name = fr.name.trim(),
                result = fr.result
            )
        }

        val normalized = text?.trim().orEmpty()
        if (normalized.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Text message must not be blank"
            )
        }
        return RunChatSessionCommand.text(sessionId, userId, normalized)
    }
}

/**
 * HITL 인터럽트 답변 입력. `id` 는 SSE 로 받은 functionCall.id 를 echo, `name` 은 기본 "adk_request_input".
 */
data class FunctionResponseInput(
    val id: String,
    val name: String = "adk_request_input",
    val result: String
)

private fun generateSessionId(): String = "session-${UUID.randomUUID()}"

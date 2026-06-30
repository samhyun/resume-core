package com.resume.core.application.usecase.write

import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import com.resume.core.application.dto.write.AgentFunctionResponse
import com.resume.core.application.dto.write.AgentMessage
import com.resume.core.application.dto.write.AgentMessagePart
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.GenerateCoverLetterCommand
import com.resume.core.application.dto.write.GenerateCoverLetterResult
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.application.dto.write.SessionIds
import com.resume.core.application.dto.write.SessionPurpose
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeoutException

/**
 * Drives the cover_letter ADK agent to completion on the server side and returns the draft.
 *
 * Flow (see adk-agent-integration-guide §3.2):
 *  1. load the resume → inject its data as session state (`resume_data`)
 *  2. send a trigger message, consume the SSE until the `adk_request_input` (cl_company_info) interrupt
 *  3. answer that interrupt with the form fields formatted into one text blob (function_response)
 *  4. let the pipeline run to completion, then read `draft_cover_letter` / `validation_result` from state
 */
@Service
class GenerateCoverLetterUseCaseService(
    private val resumeRepository: ResumeRepositoryPort,
    private val agent: AiAgentPort
) : GenerateCoverLetterUseCase {

    private val mapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(command: GenerateCoverLetterCommand): Mono<GenerateCoverLetterResult> =
        resumeRepository.findByIdAndUserId(command.resumeId, command.userId)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"))
            )
            .flatMap { resume ->
                val resumeDataJson = mapper.writeValueAsString(resume.resumeData)
                val agentSessionId = "session-${UUID.randomUUID()}"
                agent.createSession(
                    CreateSessionCommand(
                        ids = SessionIds(APP_NAME, command.userId, agentSessionId),
                        purpose = SessionPurpose.GENERAL,
                        resumeData = resumeDataJson
                    )
                ).flatMap { session -> driveAgent(command, session.agentSessionId) }
            }
            // 에이전트가 인터럽트를 안 주거나 파이프라인이 안 끝나도 무기한 대기하지 않도록 경계를 둔다.
            .timeout(GENERATION_TIMEOUT)
            .onErrorMap(TimeoutException::class.java) {
                ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Cover letter generation timed out")
            }

    private fun driveAgent(command: GenerateCoverLetterCommand, sessionId: String): Mono<GenerateCoverLetterResult> {
        val trigger = RunAgentSessionCommand(
            appName = APP_NAME,
            userId = command.userId,
            sessionId = sessionId,
            newMessage = AgentMessage("user", listOf(AgentMessagePart(text = TRIGGER_MESSAGE)))
        )

        return firstRequestInputId(agent.runSession(trigger))
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent did not request company info"))
            )
            .flatMap { interruptId ->
                val resumeRun = RunAgentSessionCommand(
                    appName = APP_NAME,
                    userId = command.userId,
                    sessionId = sessionId,
                    newMessage = AgentMessage(
                        role = "user",
                        parts = listOf(
                            AgentMessagePart(
                                functionResponse = AgentFunctionResponse(
                                    id = interruptId,
                                    name = ADK_REQUEST_INPUT,
                                    response = mapOf("result" to formatCompanyInfo(command))
                                )
                            )
                        )
                    )
                )
                // 파이프라인 완주까지 SSE를 소비한 뒤 최종 state를 읽는다.
                agent.runSession(resumeRun).then()
                    .then(agent.getSession(APP_NAME, command.userId, sessionId))
            }
            .map { session -> extractResult(command, session.stateJson) }
    }

    /** SSE에서 첫 `adk_request_input` functionCall 의 id를 찾는다(없으면 empty). */
    private fun firstRequestInputId(stream: Flux<AiAgentStreamEvent>): Mono<String> =
        stream
            .mapNotNull<String> { event -> event.data?.let { requestInputIdOrNull(it) } }
            // 인터럽트(adk_request_input)는 트리거 턴의 *종료* 이벤트다(가이드 §2.3 "이 턴은 여기서 멈춤").
            // 첫 id 에서 멈추면 (a) 인터럽트 뒤 손실할 이벤트가 없고 (b) 연결이 예상과 달리 열린 채
            // 남아도 타임아웃까지 매달리지 않는다.
            .next()

    private fun requestInputIdOrNull(data: String): String? =
        try {
            val parts = mapper.readTree(data).path("content").path("parts")
            parts.firstOrNull { it.path("functionCall").path("name").asText() == ADK_REQUEST_INPUT }
                ?.path("functionCall")?.path("id")?.asText()?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            log.debug("Failed to parse ADK SSE event for request_input id: {}", ex.message)
            null
        }

    /** 폼 필드를 cl_company_info 가 기대하는 자유 텍스트 한 덩어리로 합친다. */
    private fun formatCompanyInfo(command: GenerateCoverLetterCommand): String =
        buildList {
            add("기업명: ${command.companyName}")
            add("지원 직무: ${command.position}")
            command.jobDescription?.trim()?.takeIf { it.isNotEmpty() }?.let { add("채용공고(JD):\n$it") }
            command.companyCulture?.trim()?.takeIf { it.isNotEmpty() }?.let { add("기업 특징/문화: $it") }
        }.joinToString("\n")

    private fun extractResult(command: GenerateCoverLetterCommand, stateJson: String): GenerateCoverLetterResult {
        val state = mapper.readTree(stateJson)
        val draft = state.path("draft_cover_letter").asDictNode()
        val content = draft.path("full_text").asText("").trim().takeIf { it.isNotEmpty() }
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent returned no cover letter content")
        val score = state.path("validation_result").asDictNode().path("total_score")
            .takeIf { it.isNumber }?.asInt()
        return GenerateCoverLetterResult(
            resumeId = command.resumeId,
            companyName = command.companyName,
            position = command.position,
            jobDescription = command.jobDescription,
            content = content,
            validationScore = score
        )
    }

    /** state 값이 dict(JSON object) 또는 JSON 문자열로 올 수 있어 양쪽 모두 object 로 정규화한다. */
    private fun JsonNode.asDictNode(): JsonNode =
        if (isTextual) {
            try {
                mapper.readTree(asText())
            } catch (ex: Exception) {
                this
            }
        } else {
            this
        }

    companion object {
        private const val APP_NAME = "cover_letter"
        private const val ADK_REQUEST_INPUT = "adk_request_input"
        private const val TRIGGER_MESSAGE = "자기소개서 작성해줘"

        // 에이전트 생성은 검증 재시도 포함 ~10-20초. 넉넉한 상한.
        private val GENERATION_TIMEOUT: Duration = Duration.ofSeconds(90)
    }
}

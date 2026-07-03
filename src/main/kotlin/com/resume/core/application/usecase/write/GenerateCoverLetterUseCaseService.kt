package com.resume.core.application.usecase.write

import tools.jackson.module.kotlin.jacksonObjectMapper
import com.resume.core.application.dto.write.AgentFunctionResponse
import com.resume.core.application.dto.write.AgentMessage
import com.resume.core.application.dto.write.AgentMessagePart
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.GenerateCoverLetterCommand
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
 * Drives the cover_letter ADK agent and **relays** its pipeline SSE to the caller.
 *
 * Flow (see adk-agent-integration-guide §3.2):
 *  1. load the resume → inject its data as session state (`resume_data`)
 *  2. send a trigger message, consume the SSE internally until the `adk_request_input`
 *     (cl_company_info) interrupt — this is fast (no LLM before the prompt)
 *  3. answer that interrupt with the form fields (function_response)
 *  4. relay the resulting pipeline stream (analysis → write → validate → finalize) to the caller
 *
 * Streaming (vs. blocking + getSession) keeps the connection alive during the ~10-20s generation,
 * so it doesn't trip front-proxy request timeouts.
 */
@Service
class GenerateCoverLetterUseCaseService(
    private val resumeRepository: ResumeRepositoryPort,
    private val agent: AiAgentPort
) : GenerateCoverLetterUseCase {

    private val mapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun stream(command: GenerateCoverLetterCommand): Flux<AiAgentStreamEvent> =
        resumeRepository.findByIdAndUserId(command.resumeId, command.userId)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found"))
            )
            .flatMapMany { resume ->
                val resumeDataJson = mapper.writeValueAsString(resume.resumeData)
                val agentSessionId = "session-${UUID.randomUUID()}"
                agent.createSession(
                    CreateSessionCommand(
                        ids = SessionIds(APP_NAME, command.userId, agentSessionId),
                        purpose = SessionPurpose.GENERAL,
                        resumeData = resumeDataJson
                    )
                ).flatMapMany { session -> driveAndRelay(command, session.agentSessionId) }
            }
            // 활성 이벤트가 끊긴 채(에이전트 행) 무한 대기하지 않도록 per-event idle 타임아웃.
            .timeout(IDLE_TIMEOUT)
            .onErrorMap(TimeoutException::class.java) {
                ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Cover letter generation stalled")
            }

    private fun driveAndRelay(command: GenerateCoverLetterCommand, sessionId: String): Flux<AiAgentStreamEvent> {
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
            .flatMapMany { interruptId ->
                val answer = RunAgentSessionCommand(
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
                // 폼 데이터로 자동응답 → 파이프라인 SSE 를 가공 없이 그대로 caller 로 relay.
                agent.runSession(answer)
            }
    }

    /** SSE에서 첫 `adk_request_input` functionCall 의 id를 찾는다(없으면 empty). */
    private fun firstRequestInputId(stream: Flux<AiAgentStreamEvent>): Mono<String> =
        stream
            .mapNotNull<String> { event -> event.data?.let { requestInputIdOrNull(it) } }
            // 인터럽트(adk_request_input)는 트리거 턴의 *종료* 이벤트다(가이드 §2.3 "이 턴은 여기서 멈춤").
            // 첫 id 에서 멈추면 (a) 손실할 후속 이벤트가 없고 (b) 연결이 열린 채 남아도 매달리지 않는다.
            .next()

    private fun requestInputIdOrNull(data: String): String? =
        try {
            val parts = mapper.readTree(data).path("content").path("parts")
            parts.firstOrNull { it.path("functionCall").path("name").asString() == ADK_REQUEST_INPUT }
                ?.path("functionCall")?.path("id")?.asString()?.takeIf { it.isNotBlank() }
        } catch (ex: Exception) {
            log.debug("Failed to parse ADK SSE event for request_input id: {}", ex.message)
            null
        }

    /** 폼 필드를 cl_company_info 가 기대하는 자유 텍스트 한 덩어리로 합친다. */
    private fun formatCompanyInfo(command: GenerateCoverLetterCommand): String =
        buildList {
            add("기업명: ${command.companyName}")
            add("지원 직무: ${command.position}")
            command.jobDescription?.takeIf { it.isNotBlank() }?.let { add("채용공고(JD):\n$it") }
            command.companyCulture?.takeIf { it.isNotBlank() }?.let { add("기업 특징/문화: $it") }
        }.joinToString("\n")

    companion object {
        private const val APP_NAME = "cover_letter"
        private const val ADK_REQUEST_INPUT = "adk_request_input"
        private const val TRIGGER_MESSAGE = "자기소개서 작성해줘"

        // 이벤트 간 최대 공백(LLM 노드 한 홉) 상한 — 초과 시 행으로 보고 스트림 종료.
        private val IDLE_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}

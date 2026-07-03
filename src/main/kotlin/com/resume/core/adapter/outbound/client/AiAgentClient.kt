package com.resume.core.adapter.outbound.client

import tools.jackson.databind.JsonNode
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentSession
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class AiAgentClient(
    private val builder: WebClient.Builder,
    @param:Value("\${aiAgent.base-url:http://localhost:8000}") private val baseUrl: String
) : AiAgentPort {

    private val client = builder.baseUrl(baseUrl).build()
    private val streamResponseType = object : ParameterizedTypeReference<ServerSentEvent<String>>() {}
    private val log = LoggerFactory.getLogger(javaClass)

    override fun createSession(req: CreateSessionCommand): Mono<AiAgentSession> {
        val ids = req.ids
        // ADK 2.0 `POST /apps/.../sessions/{id}` (create_session_with_id) 는 요청 body 전체를 초기 state로 사용한다.
        // `{"state": {...}}` 래퍼로 감싸면 state.state.x 로 이중 중첩돼 에이전트가 못 읽으므로 평면으로 보낸다.
        val state = buildMap<String, Any> {
            put("purpose", req.purpose.value)
            req.resumeData?.let { put("resume_data", it) }
        }

        return client.post()
            .uri("/apps/{app}/users/{user}/sessions/{session}", ids.appName, ids.userId, ids.sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(state)
            .retrieve()
            .onStatus({ it.isError }, ::toUpstreamError)
            .bodyToMono(JsonNode::class.java)
            .map(::toAiAgentSession)
    }

    override fun runSession(req: RunAgentSessionCommand): Flux<AiAgentStreamEvent> =
        client.post()
            .uri("/run_sse")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(req)
            .retrieve()
            .onStatus({ it.isError }, ::toUpstreamError)
            .bodyToFlux(streamResponseType)
            .map { event ->
                AiAgentStreamEvent(
                    id = event.id(),
                    event = event.event(),
                    data = event.data(),
                    retry = event.retry()?.toMillis(),
                    comment = event.comment()
                )
            }

    /**
     * AI 에이전트의 4xx/5xx 를 502(Bad Gateway)로 매핑한다.
     * `createException()` 이 에러 body 를 drain/release 하므로 커넥션 누수를 막고,
     * 업스트림 상태/본문은 서버 로그로만 남겨 상세 유출을 방지한다.
     */
    private fun toUpstreamError(response: ClientResponse): Mono<out Throwable> =
        response.createException().map { ex ->
            log.warn("AI agent request failed: {}", response.statusCode())
            log.debug("AI agent error body: {}", ex.responseBodyAsString.take(500))
            ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI agent request failed")
        }

    private fun toAiAgentSession(node: JsonNode): AiAgentSession {
        val stateNode: JsonNode = node.path("state")
        val stateJson = if (stateNode.isMissingNode || stateNode.isNull) "{}" else stateNode.toString()
        // 평면 state 주입에 맞춰 단일 경로(state.purpose)로 읽는다 (구버전 이중중첩 state.state.purpose 아님).
        val purpose = stateNode.path("purpose").takeIf { !it.isMissingNode && !it.isNull }?.asString()
        val lastUpdateTime = node.path("lastUpdateTime").takeIf { it.isNumber }?.asDouble()

        return AiAgentSession(
            agentSessionId = node.path("id").asString(),
            appName = node.path("appName").asString(),
            userId = node.path("userId").asString(),
            stateJson = stateJson,
            purpose = purpose,
            lastUpdateTime = lastUpdateTime
        )
    }
}

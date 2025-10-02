package com.resume.core.adapter.outbound.client

import com.fasterxml.jackson.databind.JsonNode
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentSession
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import org.springframework.core.ParameterizedTypeReference
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class AiAgentClient(
    private val builder: WebClient.Builder,
    @param:Value("\${aiAgent.base-url:http://localhost:8000}") private val baseUrl: String
) : AiAgentPort {

    private val client = builder.baseUrl(baseUrl).build()
    private val streamResponseType = object : ParameterizedTypeReference<ServerSentEvent<String>>() {}

    override fun createSession(req: CreateSessionCommand): Mono<AiAgentSession> {
        val ids = req.ids
        val body = mapOf("state" to mapOf("purpose" to req.purpose.value))

        return client.post()
            .uri("/apps/{app}/users/{user}/sessions/{session}", ids.appName, ids.userId, ids.sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
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

    private fun toAiAgentSession(node: JsonNode): AiAgentSession {
        val stateNode: JsonNode = node.path("state")
        val stateJson = if (stateNode.isMissingNode || stateNode.isNull) "{}" else stateNode.toString()
        val purpose = stateNode.path("state").path("purpose").takeIf { !it.isMissingNode && !it.isNull }?.asText()
        val lastUpdateTime = node.path("lastUpdateTime").takeIf { it.isNumber }?.asDouble()

        return AiAgentSession(
            agentSessionId = node.path("id").asText(),
            appName = node.path("appName").asText(),
            userId = node.path("userId").asText(),
            stateJson = stateJson,
            purpose = purpose,
            lastUpdateTime = lastUpdateTime
        )
    }
}

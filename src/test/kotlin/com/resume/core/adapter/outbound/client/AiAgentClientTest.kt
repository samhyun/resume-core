package com.resume.core.adapter.outbound.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.resume.core.application.dto.write.AgentMessage
import com.resume.core.application.dto.write.AgentMessagePart
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.application.dto.write.SessionIds
import com.resume.core.application.dto.write.SessionPurpose
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier
import java.time.Duration

class AiAgentClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: AiAgentClient

    @BeforeEach
    fun setup() {
        server = MockWebServer().also { it.start() }
        val baseUrl = server.url("/").toString().removeSuffix("/")
        client = AiAgentClient(WebClient.builder(), baseUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `createSession maps response to AiAgentSession`() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": "ext-123",
                      "appName": "resume-agent",
                      "userId": "user-1",
                      "state": {"state": {"purpose": "interview_prep"}},
                      "lastUpdateTime": 42.0
                    }
                    """.trimIndent()
                )
        )

        val command = CreateSessionCommand(
            ids = SessionIds(
                appName = "resume-agent",
                userId = "user-1",
                sessionId = "client-session"
            ),
            purpose = SessionPurpose.INTERVIEW_PREP
        )

        StepVerifier.create(client.createSession(command))
            .assertNext { session ->
                assertThat(session.agentSessionId).isEqualTo("ext-123")
                assertThat(session.appName).isEqualTo("resume-agent")
                assertThat(session.userId).isEqualTo("user-1")
                assertThat(session.purpose).isEqualTo("interview_prep")
                assertThat(session.lastUpdateTime).isEqualTo(42.0)
                assertThat(session.stateJson).contains("purpose")
            }
            .verifyComplete()

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/apps/resume-agent/users/user-1/sessions/client-session")
        assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json")
    }

    @Test
    fun `runSession streams events and maps metadata`() {
        val responseBody = """
            id: evt-1
            event: delta
            data: {"text":"Hello"}
            retry: 5000

            event: end

        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody(responseBody)
        )

        val command = RunAgentSessionCommand(
            appName = "resume-agent",
            userId = "user-1",
            sessionId = "agent-session",
            newMessage = AgentMessage(
                role = "user",
                parts = listOf(AgentMessagePart(text = "Hello"))
            )
        )

        val events = client.runSession(command)
            .take(2)
            .collectList()
            .block(Duration.ofSeconds(1))!!

        assertThat(events).hasSize(2)

        val first = events[0]
        assertThat(first.id).isEqualTo("evt-1")
        assertThat(first.event).isEqualTo("delta")
        assertThat(first.data).isEqualTo("{\"text\":\"Hello\"}")
        assertThat(first.retry).isEqualTo(5000L)
        assertThat(first.comment).isNull()

        val second = events[1]
        assertThat(second.event).isEqualTo("end")
        assertThat(second.id).isNull()
        assertThat(second.data).isNull()
        assertThat(second.retry).isNull()

        val recorded = server.takeRequest()
        val body = recorded.body.readUtf8()
        val json = ObjectMapper().readTree(body)

        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/run_sse")
        assertThat(recorded.getHeader("Content-Type")).isEqualTo("application/json")
        assertThat(recorded.getHeader("Accept")).isEqualTo("text/event-stream")
        assertThat(json.path("appName").asText()).isEqualTo("resume-agent")
        assertThat(json.path("userId").asText()).isEqualTo("user-1")
        assertThat(json.path("sessionId").asText()).isEqualTo("agent-session")

        val messageNode = json.path("newMessage")
        assertThat(messageNode.path("role").asText()).isEqualTo("user")
        assertThat(messageNode.path("parts")[0].path("text").asText()).isEqualTo("Hello")
    }
}

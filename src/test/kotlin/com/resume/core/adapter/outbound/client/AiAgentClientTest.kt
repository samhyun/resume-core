package com.resume.core.adapter.outbound.client

import tools.jackson.module.kotlin.jacksonObjectMapper
import com.resume.core.application.dto.write.AgentFunctionResponse
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
                      "state": {"purpose": "interview_prep"},
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

        // ADK 2.0: 평면 state 주입(래퍼 없이). state.purpose 로 보내고 state.state 이중중첩이 아니어야 한다.
        val sentBody = jacksonObjectMapper().readTree(recorded.body.readUtf8())
        assertThat(sentBody.path("purpose").asText()).isEqualTo("interview_prep")
        assertThat(sentBody.has("state")).isFalse()
    }

    @Test
    fun `createSession injects resume_data into flat state when provided`() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": "ext-cover-1",
                      "appName": "cover_letter",
                      "userId": "user-1",
                      "state": {"purpose": "general", "resume_data": "{\"name\":\"홍길동\"}"}
                    }
                    """.trimIndent()
                )
        )

        val command = CreateSessionCommand(
            ids = SessionIds(
                appName = "cover_letter",
                userId = "user-1",
                sessionId = "client-session"
            ),
            purpose = SessionPurpose.GENERAL,
            resumeData = "{\"name\":\"홍길동\"}"
        )

        StepVerifier.create(client.createSession(command))
            .assertNext { session ->
                assertThat(session.appName).isEqualTo("cover_letter")
                assertThat(session.stateJson).contains("resume_data")
            }
            .verifyComplete()

        val recorded = server.takeRequest()
        val sentBody = jacksonObjectMapper().readTree(recorded.body.readUtf8())
        assertThat(sentBody.path("purpose").asText()).isEqualTo("general")
        assertThat(sentBody.path("resume_data").asText()).isEqualTo("{\"name\":\"홍길동\"}")
        assertThat(sentBody.has("state")).isFalse()
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
        val json = jacksonObjectMapper().readTree(body)

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

    @Test
    fun `runSession serializes function_response part for HITL resume`() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/event-stream")
                .setBody("event: end\n\n")
        )

        val command = RunAgentSessionCommand(
            appName = "interview",
            userId = "user-1",
            sessionId = "agent-session",
            newMessage = AgentMessage(
                role = "user",
                parts = listOf(
                    AgentMessagePart(
                        functionResponse = AgentFunctionResponse(
                            id = "iv_answer",
                            name = "adk_request_input",
                            response = mapOf("result" to "5년 경력입니다")
                        )
                    )
                )
            )
        )

        client.runSession(command).take(1).collectList().block(Duration.ofSeconds(1))

        val recorded = server.takeRequest()
        val json = jacksonObjectMapper().readTree(recorded.body.readUtf8())
        val part = json.path("newMessage").path("parts")[0]

        // function_response 단독 파트 — text 와 섞이면 안 됨.
        assertThat(part.has("text")).isFalse()
        assertThat(part.path("functionResponse").path("id").asText()).isEqualTo("iv_answer")
        assertThat(part.path("functionResponse").path("name").asText()).isEqualTo("adk_request_input")
        assertThat(part.path("functionResponse").path("response").path("result").asText())
            .isEqualTo("5년 경력입니다")
    }

    @Test
    fun `getSession fetches the session state via GET`() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "id": "sess-1",
                      "appName": "cover_letter",
                      "userId": "user-1",
                      "state": {"purpose": "general", "draft_cover_letter": {"full_text": "본문"}}
                    }
                    """.trimIndent()
                )
        )

        StepVerifier.create(client.getSession("cover_letter", "user-1", "sess-1"))
            .assertNext { session ->
                assertThat(session.agentSessionId).isEqualTo("sess-1")
                assertThat(session.stateJson).contains("draft_cover_letter")
            }
            .verifyComplete()

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("GET")
        assertThat(recorded.path).isEqualTo("/apps/cover_letter/users/user-1/sessions/sess-1")
    }
}

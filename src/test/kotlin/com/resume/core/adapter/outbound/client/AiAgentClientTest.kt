package com.resume.core.adapter.outbound.client

import com.resume.core.application.dto.write.CreateSessionCommand
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
}

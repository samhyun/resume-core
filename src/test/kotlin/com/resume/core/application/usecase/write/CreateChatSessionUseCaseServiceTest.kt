package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.SessionIds
import com.resume.core.application.dto.write.SessionPurpose
import com.resume.core.domain.model.ChatSession
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentSession
import com.resume.core.port.outbound.persistence.ChatSessionRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class CreateChatSessionUseCaseServiceTest {

    private lateinit var agent: AiAgentPort
    private lateinit var chatSessionRepository: ChatSessionRepositoryPort
    private lateinit var useCase: CreateChatSessionUseCaseService

    @BeforeEach
    fun setup() {
        agent = mockk()
        chatSessionRepository = mockk()
        useCase = CreateChatSessionUseCaseService(agent, chatSessionRepository)
    }

    @Test
    fun `handle should persist new session using the authenticated identity, not the upstream echo`() {
        val command = CreateSessionCommand(
            ids = SessionIds(appName = "resume-core", userId = "user-1", sessionId = "client-session"),
            purpose = SessionPurpose.INTERVIEW_PREP
        )
        // 에이전트가 소유자/앱 이름을 다른 값으로 echo 하더라도 요청 값(command.ids)을 신뢰해야 한다.
        val agentSession = AiAgentSession(
            agentSessionId = "ext-123",
            appName = "spoofed-app",
            userId = "spoofed-user",
            stateJson = "{\"purpose\":\"interview_prep\"}",
            purpose = "interview_prep",
            lastUpdateTime = 42.0
        )

        var savedSession: ChatSession? = null

        every { agent.createSession(command) } returns Mono.just(agentSession)
        every { chatSessionRepository.replaceActiveSession(any()) } answers {
            val session = firstArg<ChatSession>()
            savedSession = session
            Mono.just(session)
        }

        StepVerifier.create(useCase.handle(command))
            .assertNext { result ->
                assertThat(result.agentSessionId).isEqualTo(agentSession.agentSessionId)
                // 소유자/앱 이름은 command.ids 기준 (에이전트 echo 값이 아님)
                assertThat(result.appName).isEqualTo("resume-core")
                assertThat(result.userId).isEqualTo("user-1")
                assertThat(result.purpose).isEqualTo(agentSession.purpose)
                assertThat(result.status).isEqualTo(ChatSession.STATUS_ACTIVE)
                assertThat(result.sessionId).isNotNull()

                val saved = requireNotNull(savedSession)
                assertThat(saved.userId).isEqualTo("user-1")
                assertThat(saved.appName).isEqualTo("resume-core")
                assertThat(saved.agentSessionId).isEqualTo(agentSession.agentSessionId)
                assertThat(saved.stateJson).isEqualTo(agentSession.stateJson)
                assertThat(saved.purpose).isEqualTo(agentSession.purpose)
                assertThat(saved.lastUpdateTime).isEqualTo(agentSession.lastUpdateTime)
                assertThat(saved.status).isEqualTo(ChatSession.STATUS_ACTIVE)
            }
            .verifyComplete()

        verify(exactly = 1) { agent.createSession(command) }
        verify(exactly = 1) { chatSessionRepository.replaceActiveSession(any()) }
    }
}

package com.resume.core.application.usecase.write

import com.resume.core.adapter.outbound.persistence.command.entity.ChatSessionEntity
import com.resume.core.adapter.outbound.persistence.command.repository.ChatSessionRepository
import com.resume.core.application.dto.write.CreateSessionCommand
import com.resume.core.application.dto.write.SessionIds
import com.resume.core.application.dto.write.SessionPurpose
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentSession
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.r2dbc.postgresql.codec.Json
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
class CreateChatSessionUseCaseServiceTest {

    private lateinit var agent: AiAgentPort
    private lateinit var repository: ChatSessionRepository
    private lateinit var useCase: CreateChatSessionUseCaseService

    @BeforeEach
    fun setup() {
        agent = mockk()
        repository = mockk()
        useCase = CreateChatSessionUseCaseService(agent, repository)
    }

    @Test
    fun `handle should persist new session and return projection`() {
        val command = CreateSessionCommand(
            ids = SessionIds(appName = "resume-core", userId = "user-1", sessionId = "client-session"),
            purpose = SessionPurpose.INTERVIEW_PREP
        )
        val agentSession = AiAgentSession(
            agentSessionId = "ext-123",
            appName = "resume-core",
            userId = "user-1",
            stateJson = "{\"state\":{\"purpose\":\"interview_prep\"}}",
            purpose = "interview_prep",
            lastUpdateTime = 42.0
        )

        var savedEntity: ChatSessionEntity? = null

        every { agent.createSession(command) } returns Mono.just(agentSession)
        every { repository.closeActiveByUser("user-1") } returns Mono.just(1)
        every { repository.save(any()) } answers {
            val entity = firstArg<ChatSessionEntity>()
            savedEntity = entity
            Mono.just(entity)
        }

        StepVerifier.create(useCase.handle(command))
            .assertNext { result ->
                assertThat(result.agentSessionId).isEqualTo(agentSession.agentSessionId)
                assertThat(result.appName).isEqualTo(agentSession.appName)
                assertThat(result.userId).isEqualTo(agentSession.userId)
                assertThat(result.purpose).isEqualTo(agentSession.purpose)
                assertThat(result.status).isEqualTo("ACTIVE")
                assertThat(result.sessionId).isNotNull()

                assertThat(savedEntity).isNotNull
                assertThat(savedEntity!!.agentSessionId).isEqualTo(agentSession.agentSessionId)
                assertThat(savedEntity!!.state.asString()).isEqualTo(agentSession.stateJson)
                assertThat(savedEntity!!.purpose).isEqualTo(agentSession.purpose)
                assertThat(savedEntity!!.lastUpdateTime).isEqualTo(agentSession.lastUpdateTime)
            }
            .verifyComplete()

        verify(exactly = 1) { agent.createSession(command) }
        verify(exactly = 1) { repository.closeActiveByUser("user-1") }
        verify(exactly = 1) { repository.save(any()) }
    }
}

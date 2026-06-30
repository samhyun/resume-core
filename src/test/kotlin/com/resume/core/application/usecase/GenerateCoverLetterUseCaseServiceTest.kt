package com.resume.core.application.usecase

import com.resume.core.application.dto.write.GenerateCoverLetterCommand
import com.resume.core.application.dto.write.RunAgentSessionCommand
import com.resume.core.application.usecase.write.GenerateCoverLetterUseCaseService
import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeData
import com.resume.core.domain.model.ResumeSummary
import com.resume.core.port.outbound.external.AiAgentPort
import com.resume.core.port.outbound.external.AiAgentSession
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDateTime
import java.util.UUID

class GenerateCoverLetterUseCaseServiceTest {

    private lateinit var resumeRepository: ResumeRepositoryPort
    private lateinit var agent: AiAgentPort
    private lateinit var service: GenerateCoverLetterUseCaseService

    @BeforeEach
    fun setup() {
        resumeRepository = mockk()
        agent = mockk()
        service = GenerateCoverLetterUseCaseService(resumeRepository, agent)
    }

    @Test
    fun `auto-answers the HITL then relays the pipeline stream`() {
        val resumeId = UUID.randomUUID()
        every { resumeRepository.findByIdAndUserId(resumeId, "user-1") } returns Mono.just(sampleResume(resumeId))
        every { agent.createSession(any()) } returns Mono.just(session("sess-1"))

        val triggerEvent = AiAgentStreamEvent(
            data = """{"content":{"role":"model","parts":[{"functionCall":{"id":"call-1","name":"adk_request_input","args":{"message":"기업 정보?"}}}]}}"""
        )
        val finalizeEvent = AiAgentStreamEvent(
            id = "evt-1",
            data = """{"content":{"role":"model","parts":[{"text":"생성된 자기소개서 본문"}]}}"""
        )
        // 첫 호출(트리거)=인터럽트, 둘째 호출(function_response)=relay 대상 파이프라인 스트림
        every { agent.runSession(any()) } returnsMany listOf(Flux.just(triggerEvent), Flux.just(finalizeEvent))

        val command = GenerateCoverLetterCommand(
            userId = "user-1",
            resumeId = resumeId,
            companyName = "Acme",
            position = "Backend Engineer",
            jobDescription = "JD 내용",
            companyCulture = "수평적 문화"
        )

        // 트리거 이벤트는 내부 소비되고, 프론트로는 파이프라인(finalize) 이벤트만 relay 된다.
        StepVerifier.create(service.stream(command))
            .assertNext { event -> assertThat(event.data).contains("생성된 자기소개서 본문") }
            .verifyComplete()

        // function_response 가 받은 인터럽트 id(call-1)를 echo + 폼 데이터를 result로 전달
        val captor = mutableListOf<RunAgentSessionCommand>()
        verify { agent.runSession(capture(captor)) }
        val functionResponse = captor.last().newMessage.parts[0].functionResponse
        assertThat(functionResponse).isNotNull
        assertThat(functionResponse!!.id).isEqualTo("call-1")
        assertThat(functionResponse.response["result"].toString()).contains("기업명: Acme", "지원 직무: Backend Engineer")
    }

    @Test
    fun `resume not found yields 404`() {
        every { resumeRepository.findByIdAndUserId(any(), any()) } returns Mono.empty()

        StepVerifier.create(
            service.stream(GenerateCoverLetterCommand("user-1", UUID.randomUUID(), "Acme", "BE", null, null))
        )
            .expectErrorSatisfies {
                assertThat((it as ResponseStatusException).statusCode.value()).isEqualTo(404)
            }
            .verify()
    }

    @Test
    fun `missing company-info interrupt yields 502`() {
        val resumeId = UUID.randomUUID()
        every { resumeRepository.findByIdAndUserId(resumeId, "user-1") } returns Mono.just(sampleResume(resumeId))
        every { agent.createSession(any()) } returns Mono.just(session("sess-1"))
        every { agent.runSession(any()) } returns Flux.just(
            AiAgentStreamEvent(data = """{"content":{"role":"model","parts":[{"text":"안녕하세요"}]}}""")
        )

        StepVerifier.create(
            service.stream(GenerateCoverLetterCommand("user-1", resumeId, "Acme", "BE", null, null))
        )
            .expectErrorSatisfies {
                assertThat((it as ResponseStatusException).statusCode.value()).isEqualTo(502)
            }
            .verify()
    }

    private fun sampleResume(id: UUID): Resume =
        Resume(
            id = id,
            userId = "user-1",
            resumeData = ResumeData(name = "홍길동", summary = ResumeSummary(headline = "Backend Engineer")),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 1,
            isActive = true
        )

    private fun session(id: String): AiAgentSession =
        AiAgentSession(
            agentSessionId = id,
            appName = "cover_letter",
            userId = "user-1",
            stateJson = "{}",
            purpose = "general",
            lastUpdateTime = null
        )
}

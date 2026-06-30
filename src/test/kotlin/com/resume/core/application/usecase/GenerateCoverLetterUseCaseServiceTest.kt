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
    fun `drives agent through HITL and extracts draft from final state`() {
        val resumeId = UUID.randomUUID()
        every { resumeRepository.findByIdAndUserId(resumeId, "user-1") } returns Mono.just(sampleResume(resumeId))
        every { agent.createSession(any()) } returns Mono.just(session("sess-1"))

        val triggerEvent = AiAgentStreamEvent(
            data = """{"content":{"role":"model","parts":[{"functionCall":{"id":"call-1","name":"adk_request_input","args":{"message":"기업 정보?"}}}]}}"""
        )
        // 첫 호출(트리거)=인터럽트 이벤트, 둘째 호출(function_response)=완주(빈 스트림)
        every { agent.runSession(any()) } returnsMany listOf(Flux.just(triggerEvent), Flux.empty())

        every { agent.getSession("cover_letter", "user-1", "sess-1") } returns Mono.just(
            session(
                "sess-1",
                state = """{"draft_cover_letter":{"full_text":"생성된 자기소개서 본문"},"validation_result":{"total_score":88}}"""
            )
        )

        val command = GenerateCoverLetterCommand(
            userId = "user-1",
            resumeId = resumeId,
            companyName = "Acme",
            position = "Backend Engineer",
            jobDescription = "JD 내용",
            companyCulture = "수평적 문화"
        )

        StepVerifier.create(service.handle(command))
            .assertNext { result ->
                assertThat(result.content).isEqualTo("생성된 자기소개서 본문")
                assertThat(result.validationScore).isEqualTo(88)
                assertThat(result.companyName).isEqualTo("Acme")
                assertThat(result.resumeId).isEqualTo(resumeId)
            }
            .verifyComplete()

        // function_response 가 받은 인터럽트 id(call-1)를 echo + 폼 데이터를 result로 전달하는지 검증
        val captor = mutableListOf<RunAgentSessionCommand>()
        verify { agent.runSession(capture(captor)) }
        val functionResponse = captor.last().newMessage.parts[0].functionResponse
        assertThat(functionResponse).isNotNull
        assertThat(functionResponse!!.id).isEqualTo("call-1")
        assertThat(functionResponse.name).isEqualTo("adk_request_input")
        assertThat(functionResponse.response["result"].toString()).contains("기업명: Acme", "지원 직무: Backend Engineer")
    }

    @Test
    fun `resume not found yields 404`() {
        every { resumeRepository.findByIdAndUserId(any(), any()) } returns Mono.empty()

        StepVerifier.create(
            service.handle(GenerateCoverLetterCommand("user-1", UUID.randomUUID(), "Acme", "BE", null, null))
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
        // 트리거 스트림에 adk_request_input functionCall 이 없음 → 인터럽트 미검출
        every { agent.runSession(any()) } returns Flux.just(
            AiAgentStreamEvent(data = """{"content":{"role":"model","parts":[{"text":"안녕하세요"}]}}""")
        )

        StepVerifier.create(
            service.handle(GenerateCoverLetterCommand("user-1", resumeId, "Acme", "BE", null, null))
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

    private fun session(id: String, state: String = "{}"): AiAgentSession =
        AiAgentSession(
            agentSessionId = id,
            appName = "cover_letter",
            userId = "user-1",
            stateJson = state,
            purpose = "general",
            lastUpdateTime = null
        )
}

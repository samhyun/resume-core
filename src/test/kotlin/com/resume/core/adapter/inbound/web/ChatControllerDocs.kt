package com.resume.core.adapter.inbound.web

import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.resume.core.application.dto.write.CreateChatSessionResult
import com.resume.core.application.dto.write.SessionPurpose
import com.resume.core.application.usecase.read.StreamChatSessionUseCase
import com.resume.core.application.usecase.write.CreateChatSessionUseCase
import com.resume.core.support.docs.DocsFieldType.ENUM
import com.resume.core.support.docs.DocsFieldType.STRING
import com.resume.core.support.docs.fields
import com.resume.core.support.docs.headers
import com.resume.core.support.docs.requestFields
import com.resume.core.support.docs.requestHeaders
import com.resume.core.support.docs.responseFields
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.ExchangeStrategies
import reactor.core.publisher.Mono
import java.util.UUID

@ExtendWith(RestDocumentationExtension::class)
class ChatControllerDocs {

    private val createUseCase: CreateChatSessionUseCase = mockk()
    private val streamUseCase: StreamChatSessionUseCase = mockk(relaxed = true)

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        val mapper = jacksonObjectMapper()

        webTestClient = WebTestClient.bindToController(ChatController(createUseCase, streamUseCase))
            .configureClient()
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { configurer ->
                        configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(mapper))
                        configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(mapper))
                    }
                    .build()
            )
            .filter(
                documentationConfiguration(restDocumentation)
                    .operationPreprocessors()
                    .withRequestDefaults(Preprocessors.prettyPrint())
                    .withResponseDefaults(Preprocessors.prettyPrint())
            )
            .build()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `document create chat session`() {
        val response = CreateChatSessionResult(
            sessionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
            agentSessionId = "agent-session-123",
            appName = "resume-agent",
            userId = "user-1",
            purpose = SessionPurpose.INTERVIEW_PREP.value,
            status = "ACTIVE"
        )

        every { createUseCase.handle(any()) } returns Mono.just(response)

        val request = mapOf(
            "appName" to "resume-agent",
            "userId" to "user-1",
            "purpose" to SessionPurpose.INTERVIEW_PREP.value
        )

        webTestClient.post()
            .uri("/api/chats/sessions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .consumeWith(
                document(
                    "chat-session-create",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Chats")
                            .summary("채팅 세션 생성")
                            .description("사용자와 연결된 AI 에이전트 세션을 생성합니다")
                            .requestFields {
                                "appName" type STRING means "AI 에이전트 애플리케이션 식별자"
                                "userId" type STRING means "세션을 소유한 사용자 식별자"
                                "purpose" type ENUM(SessionPurpose::class) optional true means "세션 목적 (기본값: GENERAL)"
                            }
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseFields {
                                "sessionId" type STRING means "서버가 생성한 세션 ID"
                                "agentSessionId" type STRING means "AI 에이전트가 반환한 외부 세션 ID"
                                "appName" type STRING means "AI 에이전트 애플리케이션 식별자"
                                "userId" type STRING means "세션 소유자 ID"
                                "purpose" type STRING optional true means "설정된 세션 목적"
                                "status" type STRING means "현재 세션 상태"
                            }
                            .requestSchema(Schema("CreateChatSessionRequest"))
                            .responseSchema(Schema("CreateChatSessionResponse"))
                            .build()
                    )
                )
            )
    }
}

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
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import com.resume.core.support.docs.DocsFieldType.ENUM
import com.resume.core.support.docs.DocsFieldType.STRING
import com.resume.core.support.docs.headers
import com.resume.core.support.docs.requestHeaders
import com.resume.core.support.docs.requestFields
import com.resume.core.support.docs.responseFields
import com.resume.core.support.docs.ResourceEnricher
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import kotlin.text.Charsets
import java.util.UUID

@ExtendWith(RestDocumentationExtension::class)
class ChatControllerDocs {

    private val createUseCase: CreateChatSessionUseCase = mockk()
    private val streamUseCase: StreamChatSessionUseCase = mockk(relaxed = true)

    private val objectMapper = jacksonObjectMapper()

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        webTestClient = WebTestClient.bindToController(ChatController(createUseCase, streamUseCase))
            .configureClient()
            .exchangeStrategies(
                ExchangeStrategies.builder()
                    .codecs { configurer ->
                        configurer.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper))
                        configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper))
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
            .uri("/api/resume-core/chats/sessions")
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

    @Test
    fun `document run sse with multipart file payload`() {
        val sessionId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val events = Flux.just(
            AiAgentStreamEvent(id = "evt-1", event = "delta", data = "{\"text\":\"파일 분석 중...\"}"),
            AiAgentStreamEvent(event = "end")
        )

        every { streamUseCase.stream(any()) } returns events

        val builder = MultipartBodyBuilder()
        builder.part("sessionId", sessionId.toString())
        builder.part(
            "file",
            object : org.springframework.core.io.ByteArrayResource("resume content".toByteArray()) {
                override fun getFilename(): String = "resume.pdf"
            }
        ).contentType(MediaType.APPLICATION_PDF)
        builder.part("displayName", "이력서.pdf")

        webTestClient.post()
            .uri("/api/resume-core/chats/run-sse")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .body(BodyInserters.fromMultipartData(builder.build()))
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith { entity ->
                // Document the API with DSL
                document<ByteArray>(
                    "chat-run-sse-multipart",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Chats")
                            .summary("채팅 세션 실행 - Multipart (파일 전용)")
                            .description(
                                "기존 채팅 세션에 파일을 업로드하고 Server-Sent Events 스트림을 통해 " +
                                    "생성된 응답을 순차적으로 전달받습니다. 요청은 multipart/form-data로 sessionId, " +
                                    "file, displayName(선택) 파트를 포함해야 합니다."
                            )
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseSchema(Schema("AiAgentStreamEvent"))
                            .build()
                    )
                ).accept(entity)

                // Enrich documentation with DSL
                ResourceEnricher.enrich("chat-run-sse-multipart") {
                    request {
                        contentType("multipart/form-data")

                        formParameters {
                            parameter("sessionId", description = "실행할 채팅 세션 ID", optional = false)
                            parameter("file", description = "업로드할 파일 바이너리", optional = false, format = "binary")
                            parameter("displayName", description = "파일 표시 이름 (기본값: 원본 파일명)", optional = true)
                        }

                        requestFields {
                            field("sessionId", description = "실행할 채팅 세션 ID", optional = false)
                            field("file", description = "업로드할 파일 바이너리", optional = false, format = "binary")
                            field("displayName", description = "파일 표시 이름 (기본값: 원본 파일명)", optional = true)
                        }
                    }

                    response {
                        responseFields {
                            field("id", "SSE 이벤트 ID")
                            field("event", "SSE 이벤트 타입")
                            field("data", "SSE 데이터 페이로드 (JSON 문자열)")
                            field("retry", "재연결 지연(ms)")
                            field("comment", "SSE 코멘트")
                        }
                    }
                }

                // Assert response
                val body = entity.responseBody
                assertThat(body).isNotNull
                val text = String(body!!, Charsets.UTF_8)
                assertThat(text).contains("파일 분석 중")
            }
    }

    @Test
    fun `document run sse with json payload`() {
        val sessionId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
        val events = Flux.just(
            AiAgentStreamEvent(id = "evt-2", event = "delta", data = "{\"text\":\"JSON 응답입니다\"}"),
            AiAgentStreamEvent(event = "end")
        )

        every { streamUseCase.stream(any()) } returns events

        val request = mapOf(
            "sessionId" to sessionId.toString(),
            "text" to "JSON 요청입니다"
        )

        webTestClient.post()
            .uri("/api/resume-core/chats/run-sse")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            .expectBody()
            .consumeWith(
                document(
                    "chat-run-sse-json",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Chats")
                            .summary("채팅 세션 실행 - JSON (텍스트 전용)")
                            .description(
                                "기존 채팅 세션에 JSON 형태로 텍스트 메시지를 전송하고 Server-Sent Events 스트림을 통해 " +
                                    "생성된 응답을 순차적으로 전달받습니다. 텍스트 메시지 전용이며, 파일 업로드는 multipart 엔드포인트를 사용하세요."
                            )
                            .requestFields {
                                "sessionId" type STRING means "실행할 채팅 세션 ID"
                                "text" type STRING means "전송할 사용자 메시지"
                            }
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseSchema(Schema("AiAgentStreamEvent"))
                            .requestSchema(Schema("RunChatSessionRequest"))
                            .build()
                    )
                )
            )
    }

}

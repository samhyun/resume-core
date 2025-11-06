package com.resume.core.adapter.inbound.web

import com.epages.restdocs.apispec.ResourceDocumentation.resource
import com.epages.restdocs.apispec.ResourceSnippetParameters
import com.epages.restdocs.apispec.Schema
import com.epages.restdocs.apispec.WebTestClientRestDocumentationWrapper.document
import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.application.dto.write.SaveResumeResult
import com.resume.core.application.dto.write.UpdateResumeResult
import com.resume.core.application.usecase.read.GetActiveResumeUseCase
import com.resume.core.application.usecase.read.GetResumeUseCase
import com.resume.core.application.usecase.read.ListResumesUseCase
import com.resume.core.application.usecase.write.SaveResumeUseCase
import com.resume.core.application.usecase.write.UpdateResumeUseCase
import com.resume.core.domain.model.*
import com.resume.core.support.docs.DocsFieldType.*
import com.resume.core.support.docs.requestFields
import com.resume.core.support.docs.requestHeaders
import com.resume.core.support.docs.responseFields
import com.resume.core.support.security.MockJwtWebFilter
import org.mockito.BDDMockito.given
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.operation.preprocess.Preprocessors
import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@WebFluxTest(
    controllers = [ResumeController::class],
    excludeAutoConfiguration = [
        ReactiveOAuth2ClientAutoConfiguration::class,
        ReactiveOAuth2ResourceServerAutoConfiguration::class
    ]
)
@ContextConfiguration(classes = [ResumeController::class, TestSecurityConfig::class, ReactiveJwtAuthenticationFacade::class])
@ExtendWith(RestDocumentationExtension::class)
class ResumeControllerDocs {

    @Autowired
    lateinit var webTestClient: WebTestClient

    @MockitoBean
    lateinit var saveResumeUseCase: SaveResumeUseCase

    @MockitoBean
    lateinit var updateResumeUseCase: UpdateResumeUseCase

    @MockitoBean
    lateinit var getResumeUseCase: GetResumeUseCase

    @MockitoBean
    lateinit var listResumesUseCase: ListResumesUseCase

    @MockitoBean
    lateinit var getActiveResumeUseCase: GetActiveResumeUseCase

    @BeforeEach
    fun setUp(restDocumentation: RestDocumentationContextProvider) {
        webTestClient = webTestClient
            .mutate()
            .filter(WebTestClientRestDocumentation.documentationConfiguration(restDocumentation))
            .apply(MockJwtWebFilter.asMutator("test-user"))
            .build()
    }

    @Test
    fun `document save resume`() {
        val resumeId = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val userId = "test-user"  // 테스트에서는 JWT가 없으므로 기본값 사용

        val result = SaveResumeResult(
            resumeId = resumeId,
            userId = userId,
            version = 1
        )

        given(saveResumeUseCase.handle(org.mockito.kotlin.any())).willReturn(Mono.just(result))

        val request = sampleResumeRequest()

        webTestClient
            .post()
            .uri("/api/resume-core/resumes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .consumeWith(
                document(
                    "resume-save",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Resumes")
                            .summary("이력서 저장")
                            .description(
                                """
                                새로운 이력서를 저장합니다.
                                - 기존에 활성화된 이력서가 있을 경우 자동으로 비활성화됩니다.
                                - 새로 저장된 이력서가 활성 이력서로 설정됩니다.
                                - JWT 토큰의 sub 클레임에서 사용자 ID를 추출합니다.
                                """.trimIndent()
                            )
                            .requestFields {
                                "summary" type OBJECT means "이력서 요약 정보"
                                "summary.headline" type STRING means "한 줄 헤드라인"
                                "summary.profile" type ARRAY means "프로필 설명 목록"
                                "summary.core_strengths" type ARRAY means "핵심 역량 목록"

                                "experience" type ARRAY means "경력 사항 목록"
                                "experience[].company" type STRING means "회사명"
                                "experience[].position" type STRING means "직위"
                                "experience[].duration" type STRING means "근무 기간"
                                "experience[].summary" type STRING means "업무 요약"
                                "experience[].projects" type ARRAY means "수행 프로젝트 목록"
                                "experience[].projects[].name" type STRING means "프로젝트명"
                                "experience[].projects[].period" type STRING means "프로젝트 기간"
                                "experience[].projects[].challenge" type STRING means "해결한 과제"
                                "experience[].projects[].actions" type ARRAY means "수행한 작업 목록"
                                "experience[].projects[].results" type ARRAY means "성과 목록"
                                "experience[].projects[].technologies" type ARRAY means "사용 기술 목록"
                                "experience[].projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "experience[].projects[].links.demo" type STRING optional true means "데모 URL"
                                "experience[].projects[].links.repo" type STRING optional true means "저장소 URL"
                                "experience[].projects[].links.github" type STRING optional true means "GitHub URL"
                                "experience[].projects[].links.website" type STRING optional true means "웹사이트 URL"

                                "skills" type OBJECT means "기술 스택"
                                "skills.programming" type ARRAY means "프로그래밍 언어"
                                "skills.frameworks" type ARRAY means "프레임워크"
                                "skills.databases" type ARRAY means "데이터베이스"
                                "skills.tools" type ARRAY means "도구"
                                "skills.cloud" type ARRAY means "클라우드 플랫폼"
                                "skills.languages" type ARRAY means "언어 능력"

                                "projects" type ARRAY optional true means "개인 프로젝트 목록"
                                "projects[].name" type STRING means "프로젝트명"
                                "projects[].period" type STRING means "프로젝트 기간"
                                "projects[].type" type STRING means "프로젝트 유형"
                                "projects[].description" type STRING means "프로젝트 설명"
                                "projects[].details" type ARRAY means "세부 활동"
                                "projects[].technologies" type ARRAY means "사용 기술"
                                "projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "projects[].links.demo" type STRING optional true means "데모 URL"
                                "projects[].links.repo" type STRING optional true means "저장소 URL"
                                "projects[].links.github" type STRING optional true means "GitHub URL"
                                "projects[].links.website" type STRING optional true means "웹사이트 URL"
                                "education" type ARRAY means "학력 사항"
                                "education[].institution" type STRING means "교육 기관명"
                                "education[].degree" type STRING means "학위"
                                "education[].major" type STRING means "전공"
                                "education[].graduation_year" type STRING means "졸업 연도"
                                "education[].gpa" type STRING optional true means "학점"
                                "education[].achievements" type ARRAY optional true means "수상 및 성과"

                                "certifications_awards" type ARRAY optional true means "자격증 및 수상"
                                "certifications_awards[].name" type STRING means "자격증/수상명"
                                "certifications_awards[].issuer" type STRING means "발급 기관"
                                "certifications_awards[].date" type STRING means "취득/수상 일자"
                                "certifications_awards[].expiry" type STRING optional true means "만료 일자"

                                "additional_info" type OBJECT optional true means "추가 정보"
                                "additional_info.publications" type ARRAY optional true means "논문/출판물"
                                "additional_info.patents" type ARRAY optional true means "특허"
                                "additional_info.speaking_activities" type ARRAY optional true means "강연 활동"
                                "additional_info.target_position" type STRING optional true means "희망 직무"
                            }
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseFields {
                                "resumeId" type STRING means "생성된 이력서 ID (UUID)"
                                "userId" type STRING means "이력서 소유자 사용자 ID"
                                "version" type NUMBER means "이력서 버전 (낙관적 잠금)"
                            }
                            .requestSchema(Schema("SaveResumeRequest"))
                            .responseSchema(Schema("SaveResumeResponse"))
                            .build()
                    )
                )
            )
    }

    @Test
    fun `document update resume`() {
        val resumeId = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val userId = "test-user"

        val result = UpdateResumeResult(
            resumeId = resumeId,
            userId = userId,
            version = 2
        )

        given(updateResumeUseCase.handle(org.mockito.kotlin.any())).willReturn(Mono.just(result))

        val request = sampleResumeRequest()

        webTestClient
            .put()
            .uri("/api/resume-core/resumes/{resumeId}", resumeId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "resume-update",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Resumes")
                            .summary("이력서 수정")
                            .description(
                                """
                                기존 이력서를 수정합니다.
                                - 경로 파라미터로 대상 이력서 ID를 전달합니다.
                                - JWT 토큰의 sub 클레임에서 사용자 ID를 추출합니다.
                                """.trimIndent()
                            )
                            .requestFields {
                                "summary" type OBJECT means "이력서 요약 정보"
                                "summary.headline" type STRING means "한 줄 헤드라인"
                                "summary.profile" type ARRAY means "프로필 설명 목록"
                                "summary.core_strengths" type ARRAY means "핵심 역량 목록"

                                "experience" type ARRAY means "경력 사항 목록"
                                "experience[].company" type STRING means "회사명"
                                "experience[].position" type STRING means "직위"
                                "experience[].duration" type STRING means "근무 기간"
                                "experience[].summary" type STRING means "업무 요약"
                                "experience[].projects" type ARRAY means "수행 프로젝트 목록"
                                "experience[].projects[].name" type STRING means "프로젝트명"
                                "experience[].projects[].period" type STRING means "프로젝트 기간"
                                "experience[].projects[].challenge" type STRING means "해결한 과제"
                                "experience[].projects[].actions" type ARRAY means "수행한 작업 목록"
                                "experience[].projects[].results" type ARRAY means "성과 목록"
                                "experience[].projects[].technologies" type ARRAY means "사용 기술 목록"
                                "experience[].projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "experience[].projects[].links.demo" type STRING optional true means "데모 URL"
                                "experience[].projects[].links.repo" type STRING optional true means "저장소 URL"
                                "experience[].projects[].links.github" type STRING optional true means "GitHub URL"
                                "experience[].projects[].links.website" type STRING optional true means "웹사이트 URL"

                                "skills" type OBJECT means "기술 스택"
                                "skills.programming" type ARRAY means "프로그래밍 언어"
                                "skills.frameworks" type ARRAY means "프레임워크"
                                "skills.databases" type ARRAY means "데이터베이스"
                                "skills.tools" type ARRAY means "도구"
                                "skills.cloud" type ARRAY means "클라우드 플랫폼"
                                "skills.languages" type ARRAY means "언어 능력"

                                "projects" type ARRAY optional true means "개인 프로젝트 목록"
                                "projects[].name" type STRING means "프로젝트명"
                                "projects[].period" type STRING means "프로젝트 기간"
                                "projects[].type" type STRING means "프로젝트 유형"
                                "projects[].description" type STRING means "프로젝트 설명"
                                "projects[].details" type ARRAY means "세부 활동"
                                "projects[].technologies" type ARRAY means "사용 기술"
                                "projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "projects[].links.demo" type STRING optional true means "데모 URL"
                                "projects[].links.repo" type STRING optional true means "저장소 URL"
                                "projects[].links.github" type STRING optional true means "GitHub URL"
                                "projects[].links.website" type STRING optional true means "웹사이트 URL"
                                "education" type ARRAY means "학력 사항"
                                "education[].institution" type STRING means "교육 기관명"
                                "education[].degree" type STRING means "학위"
                                "education[].major" type STRING means "전공"
                                "education[].graduation_year" type STRING means "졸업 연도"
                                "education[].gpa" type STRING optional true means "학점"
                                "education[].achievements" type ARRAY optional true means "수상 및 성과"

                                "certifications_awards" type ARRAY optional true means "자격증 및 수상"
                                "certifications_awards[].name" type STRING means "자격증/수상명"
                                "certifications_awards[].issuer" type STRING means "발급 기관"
                                "certifications_awards[].date" type STRING means "취득/수상 일자"
                                "certifications_awards[].expiry" type STRING optional true means "만료 일자"

                                "additional_info" type OBJECT optional true means "추가 정보"
                                "additional_info.publications" type ARRAY optional true means "논문/출판물"
                                "additional_info.patents" type ARRAY optional true means "특허"
                                "additional_info.speaking_activities" type ARRAY optional true means "강연 활동"
                                "additional_info.target_position" type STRING optional true means "희망 직무"
                            }
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseFields {
                                "resumeId" type STRING means "갱신된 이력서 ID (UUID)"
                                "userId" type STRING means "이력서 소유자 사용자 ID"
                                "version" type NUMBER means "업데이트된 이력서 버전"
                            }
                            .requestSchema(Schema("UpdateResumeRequest"))
                            .responseSchema(Schema("UpdateResumeResponse"))
                            .build()
                    )
                )
            )
    }

    @Test
    fun `document list resumes`() {
        val userId = "test-user"
        val first = createSampleResume(
            resumeId = UUID.fromString("12345678-1234-1234-1234-123456789abc"),
            userId = userId
        )
        val second = first.copy(
            id = UUID.fromString("87654321-4321-4321-4321-cba987654321"),
            isActive = false,
            resumeData = first.resumeData.copy(
                summary = first.resumeData.summary.copy(headline = "백엔드 엔지니어"),
                experience = first.resumeData.experience.map { exp ->
                    exp.copy(
                        position = "Backend Developer",
                        projects = exp.projects.map { project ->
                            project.copy(name = "Notification Service")
                        }
                    )
                }
            )
        )

        given(listResumesUseCase.handle(org.mockito.kotlin.any())).willReturn(Flux.just(first, second))

        webTestClient
            .get()
            .uri("/api/resume-core/resumes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "resume-list",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Resumes")
                            .summary("이력서 목록 조회")
                            .description(
                                """
                                사용자의 모든 이력서를 생성일 역순으로 조회합니다.
                                - JWT 토큰의 sub 클레임에서 사용자 ID를 추출합니다.
                                - 가장 최근에 저장된 이력서가 목록의 첫 번째에 위치합니다.
                                """.trimIndent()
                            )
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseFields {
                                "[].resumeId" type STRING means "이력서 ID"
                                "[].userId" type STRING means "이력서 소유자 ID"
                                "[].version" type NUMBER means "이력서 버전"
                                "[].isActive" type BOOLEAN means "활성 이력서 여부"

                                "[].summary" type OBJECT means "이력서 요약"
                                "[].summary.headline" type STRING means "한 줄 헤드라인"
                                "[].summary.profile" type ARRAY means "프로필 설명"
                                "[].summary.core_strengths" type ARRAY means "핵심 역량"

                                "[].experience" type ARRAY means "경력 사항"
                                "[].experience[].company" type STRING means "회사명"
                                "[].experience[].position" type STRING means "직위"
                                "[].experience[].duration" type STRING means "근무 기간"
                                "[].experience[].summary" type STRING means "업무 요약"
                                "[].experience[].projects" type ARRAY means "수행 프로젝트 목록"
                                "[].experience[].projects[].name" type STRING means "프로젝트명"
                                "[].experience[].projects[].period" type STRING means "프로젝트 기간"
                                "[].experience[].projects[].challenge" type STRING means "해결한 과제"
                                "[].experience[].projects[].actions" type ARRAY means "수행한 작업 목록"
                                "[].experience[].projects[].results" type ARRAY means "성과 목록"
                                "[].experience[].projects[].technologies" type ARRAY means "사용 기술 목록"
                                "[].experience[].projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "[].experience[].projects[].links.demo" type STRING optional true means "데모 URL"
                                "[].experience[].projects[].links.repo" type STRING optional true means "저장소 URL"
                                "[].experience[].projects[].links.github" type STRING optional true means "GitHub URL"
                                "[].experience[].projects[].links.website" type STRING optional true means "웹사이트 URL"

                                "[].skills" type OBJECT means "기술 스택"
                                "[].skills.programming" type ARRAY means "프로그래밍 언어"
                                "[].skills.frameworks" type ARRAY means "프레임워크"
                                "[].skills.databases" type ARRAY means "데이터베이스"
                                "[].skills.tools" type ARRAY means "도구"
                                "[].skills.cloud" type ARRAY means "클라우드"
                                "[].skills.languages" type ARRAY means "언어 능력"

                                "[].education" type ARRAY means "학력"
                                "[].education[].institution" type STRING means "교육 기관명"
                                "[].education[].degree" type STRING means "학위"
                                "[].education[].major" type STRING means "전공"
                                "[].education[].graduation_year" type STRING means "졸업 연도"
                                "[].education[].gpa" type STRING optional true means "학점"
                                "[].education[].achievements" type ARRAY optional true means "수상 및 성과"

                                "[].projects" type ARRAY optional true means "개인 프로젝트"
                                "[].projects[].name" type STRING means "프로젝트명"
                                "[].projects[].period" type STRING means "프로젝트 기간"
                                "[].projects[].type" type STRING means "프로젝트 유형"
                                "[].projects[].description" type STRING means "프로젝트 설명"
                                "[].projects[].details" type ARRAY means "상세 내용"
                                "[].projects[].technologies" type ARRAY means "사용 기술"
                                "[].projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "[].projects[].links.demo" type STRING optional true means "데모 URL"
                                "[].projects[].links.repo" type STRING optional true means "저장소 URL"
                                "[].projects[].links.github" type STRING optional true means "GitHub URL"
                                "[].projects[].links.website" type STRING optional true means "웹사이트 URL"

                                "[].certifications_awards" type ARRAY optional true means "자격증/수상"
                                "[].certifications_awards[].name" type STRING means "자격증/수상명"
                                "[].certifications_awards[].issuer" type STRING means "발급 기관"
                                "[].certifications_awards[].date" type STRING means "취득/수상 일자"
                                "[].certifications_awards[].expiry" type STRING optional true means "만료 일자"

                                "[].additional_info" type OBJECT optional true means "추가 정보"
                                "[].additional_info.publications" type ARRAY optional true means "논문/출판물"
                                "[].additional_info.patents" type ARRAY optional true means "특허"
                                "[].additional_info.speaking_activities" type ARRAY optional true means "강연 활동"
                                "[].additional_info.target_position" type STRING optional true means "희망 직무"
                            }
                            .responseSchema(Schema("GetResumesResponse"))
                            .build()
                    )
                )
            )
    }

    @Test
    fun `document get resume by id`() {
        val resumeId = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val userId = "test-user"  // 테스트에서는 JWT가 없으므로 기본값 사용

        val resume = createSampleResume(resumeId, userId)

        given(getResumeUseCase.handle(org.mockito.kotlin.any())).willReturn(Mono.just(resume))

        webTestClient
            .get()
            .uri("/api/resume-core/resumes/{resumeId}", resumeId)
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "resume-get",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Resumes")
                            .summary("이력서 조회")
                            .description(
                                """
                                특정 이력서를 조회합니다.
                                - 자신의 이력서만 조회 가능합니다.
                                - 존재하지 않거나 권한이 없는 경우 404 반환
                                """.trimIndent()
                            )
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseFields {
                                "resumeId" type STRING means "이력서 ID"
                                "userId" type STRING means "이력서 소유자 ID"
                                "version" type NUMBER means "이력서 버전"
                                "isActive" type BOOLEAN means "활성 상태 여부"

                                "summary" type OBJECT means "이력서 요약"
                                "summary.headline" type STRING means "한 줄 헤드라인"
                                "summary.profile" type ARRAY means "프로필 설명"
                                "summary.core_strengths" type ARRAY means "핵심 역량"

                                "experience" type ARRAY means "경력 사항"
                                "experience[].company" type STRING means "회사명"
                                "experience[].position" type STRING means "직위"
                                "experience[].duration" type STRING means "근무 기간"
                                "experience[].summary" type STRING means "업무 요약"
                                "experience[].projects" type ARRAY means "수행 프로젝트 목록"
                                "experience[].projects[].name" type STRING means "프로젝트명"
                                "experience[].projects[].period" type STRING means "프로젝트 기간"
                                "experience[].projects[].challenge" type STRING means "해결한 과제"
                                "experience[].projects[].actions" type ARRAY means "수행한 작업 목록"
                                "experience[].projects[].results" type ARRAY means "성과 목록"
                                "experience[].projects[].technologies" type ARRAY means "사용 기술 목록"
                                "experience[].projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "experience[].projects[].links.demo" type STRING optional true means "데모 URL"
                                "experience[].projects[].links.repo" type STRING optional true means "저장소 URL"
                                "experience[].projects[].links.github" type STRING optional true means "GitHub URL"
                                "experience[].projects[].links.website" type STRING optional true means "웹사이트 URL"
                                "skills" type OBJECT means "기술 스택"
                                "skills.programming" type ARRAY means "프로그래밍 언어"
                                "skills.frameworks" type ARRAY means "프레임워크"
                                "skills.databases" type ARRAY means "데이터베이스"
                                "skills.tools" type ARRAY means "도구"
                                "skills.cloud" type ARRAY means "클라우드"
                                "skills.languages" type ARRAY means "언어 능력"

                                "education" type ARRAY means "학력"
                                "education[].institution" type STRING means "교육 기관명"
                                "education[].degree" type STRING means "학위"
                                "education[].major" type STRING means "전공"
                                "education[].graduation_year" type STRING means "졸업 연도"
                                "education[].gpa" type STRING optional true means "학점"
                                "education[].achievements" type ARRAY optional true means "수상 및 성과"
                                "projects" type ARRAY optional true means "개인 프로젝트"
                                "projects[].name" type STRING means "프로젝트명"
                                "projects[].period" type STRING means "프로젝트 기간"
                                "projects[].type" type STRING means "프로젝트 유형"
                                "projects[].description" type STRING means "프로젝트 설명"
                                "projects[].details" type ARRAY means "상세 내용"
                                "projects[].technologies" type ARRAY means "사용 기술"
                                "projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "projects[].links.demo" type STRING optional true means "데모 URL"
                                "projects[].links.repo" type STRING optional true means "저장소 URL"
                                "projects[].links.github" type STRING optional true means "GitHub URL"
                                "projects[].links.website" type STRING optional true means "웹사이트 URL"
                                "certifications_awards" type ARRAY optional true means "자격증/수상"
                                "certifications_awards[].name" type STRING means "자격증/수상명"
                                "certifications_awards[].issuer" type STRING means "발급 기관"
                                "certifications_awards[].date" type STRING means "취득/수상 일자"
                                "certifications_awards[].expiry" type STRING optional true means "만료 일자"
                                "additional_info" type OBJECT optional true means "추가 정보"
                                "additional_info.publications" type ARRAY optional true means "논문/출판물"
                                "additional_info.patents" type ARRAY optional true means "특허"
                                "additional_info.speaking_activities" type ARRAY optional true means "강연 활동"
                                "additional_info.target_position" type STRING optional true means "희망 직무"
                            }
                            .responseSchema(Schema("GetResumeResponse"))
                            .build()
                    )
                )
            )
    }

    @Test
    fun `document get active resume`() {
        val resumeId = UUID.fromString("12345678-1234-1234-1234-123456789abc")
        val userId = "test-user"  // 테스트에서는 JWT가 없으므로 기본값 사용

        val resume = createSampleResume(resumeId, userId)

        given(getActiveResumeUseCase.handle(org.mockito.kotlin.any())).willReturn(Mono.just(resume))

        webTestClient
            .get()
            .uri("/api/resume-core/resumes/active")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .consumeWith(
                document(
                    "resume-get-active",
                    Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                    Preprocessors.preprocessResponse(Preprocessors.prettyPrint()),
                    resource(
                        ResourceSnippetParameters.builder()
                            .tag("Resumes")
                            .summary("활성 이력서 조회")
                            .description(
                                """
                                현재 활성화된 이력서를 조회합니다.
                                - 사용자당 하나의 활성 이력서만 존재합니다.
                                - 활성 이력서가 없는 경우 404 반환
                                """.trimIndent()
                            )
                            .requestHeaders {
                                HttpHeaders.AUTHORIZATION header "Keycloak 발급 Bearer 토큰" optional false
                            }
                            .responseFields {
                                "resumeId" type STRING means "이력서 ID"
                                "userId" type STRING means "이력서 소유자 ID"
                                "version" type NUMBER means "이력서 버전"
                                "isActive" type BOOLEAN means "활성 상태 (항상 true)"

                                "summary" type OBJECT means "이력서 요약"
                                "summary.headline" type STRING means "한 줄 헤드라인"
                                "summary.profile" type ARRAY means "프로필 설명"
                                "summary.core_strengths" type ARRAY means "핵심 역량"

                                "experience" type ARRAY means "경력 사항"
                                "experience[].company" type STRING means "회사명"
                                "experience[].position" type STRING means "직위"
                                "experience[].duration" type STRING means "근무 기간"
                                "experience[].summary" type STRING means "업무 요약"
                                "experience[].projects" type ARRAY means "수행 프로젝트 목록"
                                "experience[].projects[].name" type STRING means "프로젝트명"
                                "experience[].projects[].period" type STRING means "프로젝트 기간"
                                "experience[].projects[].challenge" type STRING means "해결한 과제"
                                "experience[].projects[].actions" type ARRAY means "수행한 작업 목록"
                                "experience[].projects[].results" type ARRAY means "성과 목록"
                                "experience[].projects[].technologies" type ARRAY means "사용 기술 목록"
                                "experience[].projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "experience[].projects[].links.demo" type STRING optional true means "데모 URL"
                                "experience[].projects[].links.repo" type STRING optional true means "저장소 URL"
                                "experience[].projects[].links.github" type STRING optional true means "GitHub URL"
                                "experience[].projects[].links.website" type STRING optional true means "웹사이트 URL"

                                "skills" type OBJECT means "기술 스택"
                                "skills.programming" type ARRAY means "프로그래밍 언어"
                                "skills.frameworks" type ARRAY means "프레임워크"
                                "skills.databases" type ARRAY means "데이터베이스"
                                "skills.tools" type ARRAY means "도구"
                                "skills.cloud" type ARRAY means "클라우드"
                                "skills.languages" type ARRAY means "언어 능력"

                                "education" type ARRAY means "학력"
                                "education[].institution" type STRING means "교육 기관명"
                                "education[].degree" type STRING means "학위"
                                "education[].major" type STRING means "전공"
                                "education[].graduation_year" type STRING means "졸업 연도"
                                "education[].gpa" type STRING optional true means "학점"
                                "education[].achievements" type ARRAY optional true means "수상 및 성과"

                                "projects" type ARRAY optional true means "개인 프로젝트"
                                "projects[].name" type STRING means "프로젝트명"
                                "projects[].period" type STRING means "프로젝트 기간"
                                "projects[].type" type STRING means "프로젝트 유형"
                                "projects[].description" type STRING means "프로젝트 설명"
                                "projects[].details" type ARRAY means "상세 내용"
                                "projects[].technologies" type ARRAY means "사용 기술"
                                "projects[].links" type OBJECT optional true means "프로젝트 링크"
                                "projects[].links.demo" type STRING optional true means "데모 URL"
                                "projects[].links.repo" type STRING optional true means "저장소 URL"
                                "projects[].links.github" type STRING optional true means "GitHub URL"
                                "projects[].links.website" type STRING optional true means "웹사이트 URL"

                                "certifications_awards" type ARRAY optional true means "자격증/수상"
                                "certifications_awards[].name" type STRING means "자격증/수상명"
                                "certifications_awards[].issuer" type STRING means "발급 기관"
                                "certifications_awards[].date" type STRING means "취득/수상 일자"
                                "certifications_awards[].expiry" type STRING optional true means "만료 일자"

                                "additional_info" type OBJECT optional true means "추가 정보"
                                "additional_info.publications" type ARRAY optional true means "논문/출판물"
                                "additional_info.patents" type ARRAY optional true means "특허"
                                "additional_info.speaking_activities" type ARRAY optional true means "강연 활동"
                                "additional_info.target_position" type STRING optional true means "희망 직무"
                            }
                            .responseSchema(Schema("GetResumeResponse"))
                            .build()
                    )
                )
            )
    }

    private fun sampleResumeRequest(): Map<String, Any> = mapOf(
        "summary" to mapOf(
            "headline" to "Full Stack Developer with 5+ years experience",
            "profile" to listOf(
                "Experienced in building scalable web applications",
                "Strong background in both frontend and backend development"
            ),
            "core_strengths" to listOf("Java", "Kotlin", "React", "Spring Boot")
        ),
        "experience" to listOf(
            mapOf(
                "company" to "Tech Corp",
                "position" to "Senior Developer",
                "duration" to "2020.01 - 현재",
                "summary" to "Led development of microservices architecture",
                "projects" to listOf(
                    mapOf(
                        "name" to "E-commerce Platform",
                        "period" to "2020.03 - 2021.06",
                        "challenge" to "Legacy monolith migration to microservices",
                        "actions" to listOf(
                            "Designed domain-driven architecture",
                            "Implemented event-driven communication"
                        ),
                        "results" to listOf(
                            "Reduced deployment time by 70%",
                            "Improved system scalability"
                        ),
                        "technologies" to listOf("Spring Boot", "Kafka", "Docker"),
                        "links" to mapOf(
                            "demo" to "https://demo.example.com",
                            "repo" to "https://repo.example.com",
                            "github" to "https://github.com/example/project",
                            "website" to "https://project.example.com"
                        )
                    )
                )
            )
        ),
        "skills" to mapOf(
            "programming" to listOf("Java", "Kotlin", "TypeScript"),
            "frameworks" to listOf("Spring Boot", "React", "Vue.js"),
            "databases" to listOf("PostgreSQL", "MongoDB", "Redis"),
            "tools" to listOf("Git", "Docker", "Kubernetes"),
            "cloud" to listOf("AWS", "GCP"),
            "languages" to listOf("한국어 (Native)", "English (Professional)")
        ),
        "projects" to listOf(
            mapOf(
                "name" to "Personal Portfolio",
                "period" to "2019.01 - 2019.06",
                "type" to "SIDE",
                "description" to "개인 포트폴리오 웹사이트",
                "details" to listOf("디자인 기획", "React 기반 프론트엔드 개발"),
                "technologies" to listOf("React", "TypeScript", "Netlify"),
                "links" to mapOf(
                    "demo" to "https://portfolio.example.com",
                    "repo" to "https://github.com/example/portfolio"
                )
            )
        ),
        "education" to listOf(
            mapOf(
                "institution" to "Seoul University",
                "degree" to "Bachelor",
                "major" to "Computer Science",
                "graduation_year" to "2018",
                "gpa" to "3.8/4.0",
                "achievements" to listOf("Dean's List", "Best Project Award")
            )
        ),
        "certifications_awards" to listOf(
            mapOf(
                "name" to "AWS Solutions Architect",
                "issuer" to "Amazon Web Services",
                "date" to "2021-06-15",
                "expiry" to "2024-06-15"
            )
        ),
        "additional_info" to mapOf(
            "publications" to listOf("Reactive Systems Journal, 2022"),
            "patents" to listOf("KR-1234-5678"),
            "speaking_activities" to listOf("PyCon Korea 2023"),
            "target_position" to "Senior Backend Engineer"
        )
    )

    private fun createSampleResume(resumeId: UUID, userId: String): Resume {
        return Resume(
            id = resumeId,
            userId = userId,
            resumeData = ResumeData(
                summary = ResumeSummary(
                    headline = "Full Stack Developer with 5+ years experience",
                    profile = listOf(
                        "Experienced in building scalable web applications",
                        "Strong background in both frontend and backend development"
                    ),
                    coreStrengths = listOf("Java", "Kotlin", "React", "Spring Boot")
                ),
                experience = listOf(
                    Experience(
                        company = "Tech Corp",
                        position = "Senior Developer",
                        duration = "2020.01 - 현재",
                        summary = "Led development of microservices architecture",
                        projects = listOf(
                            Project(
                                name = "E-commerce Platform",
                                period = "2020.03 - 2021.06",
                                challenge = "Legacy monolith migration to microservices",
                                actions = listOf(
                                    "Designed domain-driven architecture",
                                    "Implemented event-driven communication"
                                ),
                                results = listOf(
                                    "Reduced deployment time by 70%",
                                    "Improved system scalability"
                                ),
                                technologies = listOf("Spring Boot", "Kafka", "Docker"),
                                links = ResumeLink(github = "https://github.com/example/project")
                            )
                        )
                    )
                ),
                skills = Skills(
                    programming = listOf("Java", "Kotlin", "TypeScript"),
                    frameworks = listOf("Spring Boot", "React", "Vue.js"),
                    databases = listOf("PostgreSQL", "MongoDB", "Redis"),
                    tools = listOf("Git", "Docker", "Kubernetes"),
                    cloud = listOf("AWS", "GCP"),
                    languages = listOf("한국어 (Native)", "English (Professional)")
                ),
                projects = listOf(
                    IndependentProject(
                        name = "Personal Portfolio",
                        period = "2019.01 - 2019.06",
                        type = "SIDE",
                        description = "개인 포트폴리오 웹사이트",
                        details = listOf("디자인 기획", "React 기반 프론트엔드 개발"),
                        technologies = listOf("React", "TypeScript", "Netlify"),
                        links = ResumeLink(
                            demo = "https://portfolio.example.com",
                            repo = "https://github.com/example/portfolio"
                        )
                    )
                ),
                education = listOf(
                    Education(
                        institution = "Seoul University",
                        degree = "Bachelor",
                        major = "Computer Science",
                        graduationYear = "2018",
                        gpa = "3.8/4.0",
                        achievements = listOf("Dean's List", "Best Project Award")
                    )
                ),
                certificationsAwards = listOf(
                    CertificationAward(
                        name = "AWS Solutions Architect",
                        issuer = "Amazon Web Services",
                        date = "2021-06-15",
                        expiry = "2024-06-15"
                    )
                ),
                additionalInfo = AdditionalInfo(
                    targetPosition = "Senior Backend Engineer"
                )
            ),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 1,
            isActive = true
        )
    }
}

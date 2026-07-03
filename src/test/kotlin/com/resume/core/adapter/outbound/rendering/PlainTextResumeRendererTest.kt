package com.resume.core.adapter.outbound.rendering

import com.resume.core.domain.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PlainTextResumeRendererTest {

    private val renderer = PlainTextResumeRenderer()

    @Test
    fun `renders populated sections and skips empty ones`() {
        val resume = resumeOf(
            ResumeData(
                name = "홍길동",
                email = "hong@example.com",
                phone = "010-1234-5678",
                summary = ResumeSummary(
                    headline = "Senior Backend Engineer",
                    profile = listOf("10년 경력"),
                    coreStrengths = listOf("Kotlin", "Reactive")
                ),
                experience = listOf(
                    Experience(
                        company = "Acme",
                        position = "Engineer",
                        duration = "2020-2024",
                        summary = "결제 시스템 구축",
                        projects = listOf(
                            Project(
                                name = "Project X",
                                period = "2023",
                                challenge = "확장성",
                                actions = listOf("팀 리드"),
                                results = listOf("출시"),
                                technologies = listOf("Kotlin")
                            )
                        )
                    )
                ),
                skills = Skills(programming = listOf("Kotlin"), frameworks = listOf("Spring")),
                education = listOf(
                    Education(institution = "Uni", degree = "BS", major = "CS", graduationYear = "2012")
                )
            )
        )

        val text = renderer.render(resume)

        // 헤더 + 연락처
        assertThat(text).contains("홍길동")
        assertThat(text).contains("hong@example.com | 010-1234-5678")
        // 채워진 섹션
        assertThat(text).contains("== 요약 ==", "Senior Backend Engineer", "핵심 역량: Kotlin, Reactive")
        assertThat(text).contains("== 경력 ==", "Acme · Engineer (2020-2024)", "• Project X (2023)", "- 기술: Kotlin")
        assertThat(text).contains("== 스킬 ==", "프로그래밍: Kotlin", "프레임워크: Spring")
        assertThat(text).contains("== 학력 ==", "Uni — BS, CS (2012)")
        // 비어 있는 섹션은 나타나지 않음
        assertThat(text).doesNotContain("== 수상 ==", "== 어학 ==", "== 출판/논문 ==", "== 자격증 ==")
    }

    @Test
    fun `minimal resume renders only the summary section`() {
        val resume = resumeOf(
            ResumeData(summary = ResumeSummary(headline = "Headline Only"))
        )

        val text = renderer.render(resume)

        assertThat(text).contains("== 요약 ==", "Headline Only")
        assertThat(text).doesNotContain("== 경력 ==", "== 스킬 ==", "== 학력 ==")
    }

    @Test
    fun `drops items with no displayable content`() {
        val resume = resumeOf(
            ResumeData(
                summary = ResumeSummary(headline = "Engineer"),
                experience = listOf(
                    Experience(company = "  ", position = null, summary = null), // 표시할 내용 없음 → 제외
                    Experience(company = "RealCo", summary = "실제 내용")
                ),
                awards = listOf(Award(title = "   ")) // 빈 항목만 → 섹션 통째로 제거
            )
        )

        val text = renderer.render(resume)

        assertThat(text).contains("== 경력 ==", "RealCo")
        // 빈 항목은 헤더/불릿만 남기지 않고, 빈 항목뿐인 섹션은 사라진다.
        assertThat(text).doesNotContain("== 수상 ==")
        assertThat(text.lines()).noneMatch { it.trim() == "•" }
    }

    @Test
    fun `keeps content of an item whose name is blank without leaving a bare bullet`() {
        val resume = resumeOf(
            ResumeData(
                summary = ResumeSummary(headline = "Engineer"),
                personalProjects = listOf(
                    IndependentProject(name = "   ", description = "익명 프로젝트 설명", technologies = listOf("Kotlin"))
                )
            )
        )

        val text = renderer.render(resume)

        // 제목이 없어도 내용은 보존하되, 빈 불릿(•)만 있는 줄은 남기지 않는다.
        assertThat(text).contains("== 프로젝트 ==", "익명 프로젝트 설명", "기술: Kotlin")
        assertThat(text.lines()).noneMatch { it.trim() == "•" }
    }

    private fun resumeOf(data: ResumeData): Resume =
        Resume(
            id = UUID.randomUUID(),
            userId = "user-1",
            resumeData = data,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            version = 1,
            isActive = true
        )
}

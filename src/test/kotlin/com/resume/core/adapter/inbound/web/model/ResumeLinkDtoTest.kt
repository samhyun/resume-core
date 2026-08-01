package com.resume.core.adapter.inbound.web.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class ResumeLinkDtoTest {

    @Test
    fun `fromRaw returns empty dto for empty list`() {
        val dto = ResumeLinkDto.fromRaw(emptyList<String>())

        assertThat(dto).isEqualTo(ResumeLinkDto())
    }

    @Test
    fun `fromRaw maps a single github url to the github slot`() {
        val dto = ResumeLinkDto.fromRaw(listOf("https://github.com/hong/resume"))

        assertThat(dto.github).isEqualTo("https://github.com/hong/resume")
        assertThat(dto.repo).isNull()
        assertThat(dto.demo).isNull()
        assertThat(dto.website).isNull()
    }

    @Test
    fun `fromRaw puts a non-repo url into demo alongside github`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf("https://demo.example.com", "https://github.com/hong/resume")
        )

        assertThat(dto.github).isEqualTo("https://github.com/hong/resume")
        assertThat(dto.demo).isEqualTo("https://demo.example.com")
        assertThat(dto.repo).isNull()
        assertThat(dto.website).isNull()
    }

    @Test
    fun `fromRaw fills all four slots from github gitlab and two plain urls`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf(
                "https://demo.example.com",
                "https://github.com/hong/resume",
                "https://gitlab.com/hong/resume",
                "https://hong.dev"
            )
        )

        assertThat(dto.github).isEqualTo("https://github.com/hong/resume")
        assertThat(dto.repo).isEqualTo("https://gitlab.com/hong/resume")
        assertThat(dto.demo).isEqualTo("https://demo.example.com")
        assertThat(dto.website).isEqualTo("https://hong.dev")
    }

    @Test
    fun `fromRaw detects github without a scheme and with a www prefix`() {
        assertThat(ResumeLinkDto.fromRaw(listOf("github.com/hong/resume")).github)
            .isEqualTo("github.com/hong/resume")
        assertThat(ResumeLinkDto.fromRaw(listOf("www.github.com/hong/resume")).github)
            .isEqualTo("www.github.com/hong/resume")
        assertThat(ResumeLinkDto.fromRaw(listOf("https://GitHub.com/hong/resume")).github)
            .isEqualTo("https://GitHub.com/hong/resume")
        assertThat(ResumeLinkDto.fromRaw(listOf("https://gist.github.com/hong/1234")).github)
            .isEqualTo("https://gist.github.com/hong/1234")
    }

    @Test
    fun `fromRaw does not treat lookalike hosts or query strings as github`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf("https://github.com.evil.com/hong", "https://example.com/?next=github.com")
        )

        // 호스트가 아닌 위치에 github.com이 들어간 URL은 github 슬롯을 차지하지 못한다
        assertThat(dto.github).isNull()
        assertThat(dto.repo).isNull()
        assertThat(dto.demo).isEqualTo("https://github.com.evil.com/hong")
        assertThat(dto.website).isEqualTo("https://example.com/?next=github.com")
    }

    @Test
    fun `fromRaw does not treat github io pages as github`() {
        val dto = ResumeLinkDto.fromRaw(listOf("https://hong.github.io/portfolio"))

        assertThat(dto.github).isNull()
        assertThat(dto.demo).isEqualTo("https://hong.github.io/portfolio")
    }

    @Test
    fun `fromRaw matches repo hosts by host not by substring`() {
        val subdomain = ResumeLinkDto.fromRaw(listOf("https://git.gitlab.com/hong/resume"))
        assertThat(subdomain.repo).isEqualTo("https://git.gitlab.com/hong/resume")

        val lookalike = ResumeLinkDto.fromRaw(listOf("https://gitlab.com.evil.com/hong"))
        assertThat(lookalike.repo).isNull()
        assertThat(lookalike.demo).isEqualTo("https://gitlab.com.evil.com/hong")
    }

    @Test
    fun `fromRaw does not throw on malformed values`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf("http://", ":::", "이건 URL이 아닙니다", "https://github.com/hong")
        )

        assertThat(dto.github).isEqualTo("https://github.com/hong")
        assertThat(dto.demo).isEqualTo("http://")
        // `:::`는 스킴을 판별할 수 없어 버려지고, 스킴 없는 문자열이 남은 슬롯을 채운다
        assertThat(dto.website).isEqualTo("이건 URL이 아닙니다")
    }

    @Test
    fun `fromRaw ignores userinfo tricks and port numbers`() {
        // github.com이 userinfo 자리에 있으면 실제 호스트는 evil.com이다
        assertThat(ResumeLinkDto.fromRaw(listOf("https://github.com@evil.com/hong")).github).isNull()
        assertThat(ResumeLinkDto.fromRaw(listOf("https://github.com:8443/hong")).github)
            .isEqualTo("https://github.com:8443/hong")
    }

    @Test
    fun `fromRaw ignores backslash tricks and opaque uri schemes`() {
        // 백슬래시를 그대로 두면 authority 경계가 밀려 실제 호스트(evil.com)를 속일 수 있다
        assertThat(ResumeLinkDto.fromRaw(listOf("""https://evil.com\@github.com/path""")).github).isNull()
        // mailto는 허용 스킴이 아니라 어느 슬롯에도 담기지 않는다
        assertThat(ResumeLinkDto.fromRaw(listOf("mailto:user@github.com"))).isEqualTo(ResumeLinkDto())
    }

    @Test
    fun `fromRaw drops urls with a disallowed scheme`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf(
                "javascript://github.com/%0Aalert(1)",
                "data:text/html,<script>alert(1)</script>",
                "file:///etc/passwd",
                "vbscript:alert(1)"
            )
        )

        // 프론트의 <a href>와 PDF 템플릿으로 그대로 흘러가므로 http·https 외 스킴은 저장하지 않는다
        assertThat(dto).isEqualTo(ResumeLinkDto())
    }

    @Test
    fun `fromRaw keeps safe urls next to a disallowed scheme`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf("javascript://github.com/%0Aalert(1)", "https://github.com/hong/resume")
        )

        assertThat(dto.github).isEqualTo("https://github.com/hong/resume")
        assertThat(dto.demo).isNull()
        assertThat(dto.repo).isNull()
        assertThat(dto.website).isNull()
    }

    @Test
    fun `fromRaw allows http and scheme-less urls`() {
        assertThat(ResumeLinkDto.fromRaw(listOf("http://github.com/hong/resume")).github)
            .isEqualTo("http://github.com/hong/resume")
        assertThat(ResumeLinkDto.fromRaw(listOf("github.com/hong/resume")).github)
            .isEqualTo("github.com/hong/resume")
        assertThat(ResumeLinkDto.fromRaw(listOf("github.com:8443/hong")).github)
            .isEqualTo("github.com:8443/hong")
    }

    @Test
    fun `fromRaw drops object values with a disallowed scheme`() {
        val dto = ResumeLinkDto.fromRaw(
            mapOf(
                "github" to "javascript:alert(1)",
                "repo" to "data:text/html,<script>alert(1)</script>",
                "demo" to "https://demo.example.com",
                "website" to "hong.dev"
            )
        )

        assertThat(dto.github).isNull()
        assertThat(dto.repo).isNull()
        assertThat(dto.demo).isEqualTo("https://demo.example.com")
        assertThat(dto.website).isEqualTo("hong.dev")
    }

    @Test
    fun `fromRaw recognizes bitbucket as a repo host`() {
        val dto = ResumeLinkDto.fromRaw(listOf("https://bitbucket.org/hong/resume"))

        assertThat(dto.repo).isEqualTo("https://bitbucket.org/hong/resume")
        assertThat(dto.github).isNull()
    }

    @Test
    fun `fromRaw routes a second github url into the repo slot`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf("https://github.com/hong", "https://github.com/hong/resume")
        )

        assertThat(dto.github).isEqualTo("https://github.com/hong")
        assertThat(dto.repo).isEqualTo("https://github.com/hong/resume")
    }

    @Test
    fun `fromRaw drops overflow urls without throwing`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf(
                "https://github.com/hong/resume",
                "https://gitlab.com/hong/resume",
                "https://demo.example.com",
                "https://hong.dev",
                "https://blog.example.com",
                "https://extra.example.com"
            )
        )

        // 슬롯 네 개는 앞에서부터 채워지고, 다섯 번째 이후 URL은 담을 곳이 없어 버려진다
        assertThat(dto.github).isEqualTo("https://github.com/hong/resume")
        assertThat(dto.repo).isEqualTo("https://gitlab.com/hong/resume")
        assertThat(dto.demo).isEqualTo("https://demo.example.com")
        assertThat(dto.website).isEqualTo("https://hong.dev")
    }

    @Test
    fun `fromRaw ignores blanks duplicates and non-string elements`() {
        val dto = ResumeLinkDto.fromRaw(
            listOf("  https://hong.dev  ", "   ", "https://hong.dev", 42, null)
        )

        assertThat(dto.demo).isEqualTo("https://hong.dev")
        assertThat(dto.website).isNull()
        assertThat(dto.github).isNull()
        assertThat(dto.repo).isNull()
    }

    @Test
    fun `fromRaw maps object form by key`() {
        val dto = ResumeLinkDto.fromRaw(
            mapOf(
                "demo" to "https://demo.example.com",
                "repo" to "https://gitlab.com/hong/resume",
                "github" to "https://github.com/hong/resume",
                "website" to "  https://hong.dev  "
            )
        )

        assertThat(dto.demo).isEqualTo("https://demo.example.com")
        assertThat(dto.repo).isEqualTo("https://gitlab.com/hong/resume")
        assertThat(dto.github).isEqualTo("https://github.com/hong/resume")
        assertThat(dto.website).isEqualTo("https://hong.dev")
    }

    @Test
    fun `fromRaw returns empty dto for null and unsupported shapes`() {
        assertThat(ResumeLinkDto.fromRaw(null)).isEqualTo(ResumeLinkDto())
        assertThat(ResumeLinkDto.fromRaw("https://hong.dev")).isEqualTo(ResumeLinkDto())
    }

    @Test
    fun `deserializes both array and object json shapes`() {
        val mapper = jacksonObjectMapper()

        val fromArray = mapper.readValue<ResumeLinkDto>(
            """["https://github.com/hong/resume", "https://demo.example.com"]"""
        )
        val fromObject = mapper.readValue<ResumeLinkDto>(
            """{"github": "https://github.com/hong/resume", "demo": "https://demo.example.com"}"""
        )

        assertThat(fromArray).isEqualTo(fromObject)
        assertThat(fromArray.github).isEqualTo("https://github.com/hong/resume")
        assertThat(fromArray.demo).isEqualTo("https://demo.example.com")
    }
}

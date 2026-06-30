package com.resume.core.adapter.outbound.rendering

import com.resume.core.domain.model.Award
import com.resume.core.domain.model.CertificationAward
import com.resume.core.domain.model.Education
import com.resume.core.domain.model.Experience
import com.resume.core.domain.model.IndependentProject
import com.resume.core.domain.model.Project
import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeData
import com.resume.core.domain.model.ResumeSummary
import com.resume.core.domain.model.Skills
import com.resume.core.domain.model.Publication
import com.resume.core.port.outbound.rendering.ResumePlainTextRendererPort
import org.springframework.stereotype.Component

/**
 * Renders a [Resume] into a readable plain-text layout straight from its structured data.
 *
 * Pure and deterministic. Emptiness is checked at the *item* level, not just the list level:
 * an item with no displayable content is dropped, and a section with no surviving items
 * disappears entirely. No HTML, no external binary.
 */
@Component
class PlainTextResumeRenderer : ResumePlainTextRendererPort {

    override fun render(resume: Resume): String {
        val data = resume.resumeData
        val sections = listOfNotNull(
            header(data),
            summary(data.summary),
            experience(data.experience),
            projects(data),
            skills(data.skills),
            education(data.education),
            certifications(data.certificationsAwards),
            awards(data.awards),
            languages(data.languages),
            publications(data.publications)
        )
        return sections.joinToString("\n\n").trim() + "\n"
    }

    private fun header(data: ResumeData): String? {
        val name = data.name.clean()
        val contact = listOfNotNull(data.email.clean(), data.phone.clean(), data.address.clean())
        if (name == null && contact.isEmpty()) return null
        return buildString {
            name?.let { appendLine(it) }
            if (contact.isNotEmpty()) append(contact.joinToString(" | "))
        }.trimEnd()
    }

    private fun summary(summary: ResumeSummary): String? {
        val body = buildString {
            summary.headline.clean()?.let { appendLine(it) }
            summary.profile.clean().forEach { appendLine("- $it") }
            summary.coreStrengths.clean().takeIf { it.isNotEmpty() }
                ?.let { append("핵심 역량: ${it.joinToString(", ")}") }
        }.trimEnd()
        return section("요약", body)
    }

    private fun experience(items: List<Experience>): String? {
        val blocks = items.mapNotNull { exp ->
            val head = listOfNotNull(exp.company.clean(), exp.position.clean()).joinToString(" · ")
            val summary = exp.summary.clean()
            val projects = exp.projects.mapNotNull { renderProject(it) }
            if (head.isBlank() && summary == null && projects.isEmpty()) return@mapNotNull null
            val duration = exp.duration.clean()?.let { " ($it)" }.orEmpty()
            buildString {
                appendLine("$head$duration".trim())
                summary?.let { appendLine("  $it") }
                projects.forEach { append(it) }
            }.trimEnd()
        }
        return section("경력", blocks.takeIf { it.isNotEmpty() }?.joinToString("\n\n"))
    }

    private fun renderProject(project: Project): String? {
        val name = project.name.clean()
        val challenge = project.challenge.clean()
        val actions = project.actions.clean()
        val results = project.results.clean()
        val tech = project.technologies.clean()
        if (name == null && challenge == null && actions.isEmpty() && results.isEmpty() && tech.isEmpty()) return null
        val period = project.period.clean()?.let { " ($it)" }.orEmpty()
        return buildString {
            // 제목이 없으면 빈 불릿 줄을 남기지 않고 내용만 출력한다(내용은 보존).
            name?.let { appendLine("  • $it$period".trimEnd()) }
            challenge?.let { appendLine("    - 도전: $it") }
            actions.takeIf { it.isNotEmpty() }?.let { appendLine("    - 한 일: ${it.joinToString("; ")}") }
            results.takeIf { it.isNotEmpty() }?.let { appendLine("    - 성과: ${it.joinToString("; ")}") }
            tech.takeIf { it.isNotEmpty() }?.let { appendLine("    - 기술: ${it.joinToString(", ")}") }
        }
    }

    private fun projects(data: ResumeData): String? {
        val items = data.personalProjects + (data.projects ?: emptyList())
        val blocks = items.mapNotNull { renderIndependentProject(it) }
        return section("프로젝트", blocks.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun renderIndependentProject(project: IndependentProject): String? {
        val name = project.name.clean()
        val description = project.description.clean()
        val details = project.details.clean()
        val tech = project.technologies.clean()
        if (name == null && description == null && details.isEmpty() && tech.isEmpty()) return null
        val period = project.period.clean()?.let { " ($it)" }.orEmpty()
        return buildString {
            // 제목이 없으면 빈 불릿 줄을 남기지 않고 내용만 출력한다(내용은 보존).
            name?.let { appendLine("• $it$period".trimEnd()) }
            description?.let { appendLine("  $it") }
            details.forEach { appendLine("  - $it") }
            tech.takeIf { it.isNotEmpty() }?.let { appendLine("  기술: ${it.joinToString(", ")}") }
        }.trimEnd()
    }

    private fun skills(skills: Skills): String? {
        val rows = listOfNotNull(
            skillRow("프로그래밍", skills.programming),
            skillRow("프레임워크", skills.frameworks),
            skillRow("데이터베이스", skills.databases),
            skillRow("도구", skills.tools),
            skillRow("클라우드", skills.cloud),
            skillRow("언어", skills.languages),
            skillRow("소프트스킬", skills.softSkills),
            skillRow("기타", skills.other)
        )
        return section("스킬", rows.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun skillRow(label: String, items: List<String>): String? =
        items.clean().takeIf { it.isNotEmpty() }?.let { "$label: ${it.joinToString(", ")}" }

    private fun education(items: List<Education>): String? {
        val blocks = items.mapNotNull { edu ->
            val institution = edu.institution.clean()
            val degreeMajor = listOfNotNull(edu.degree.clean(), edu.major.clean()).joinToString(", ")
            val gpa = edu.gpa.clean()
            val achievements = edu.achievements?.clean().orEmpty()
            if (institution == null && degreeMajor.isBlank() && gpa == null && achievements.isEmpty()) {
                return@mapNotNull null
            }
            val tail = edu.graduationYear.clean()?.let { " ($it)" }.orEmpty()
            val main = listOfNotNull(institution, degreeMajor.takeIf { it.isNotBlank() }).joinToString(" — ")
            buildString {
                appendLine("• $main$tail".trimEnd())
                gpa?.let { appendLine("  GPA: $it") }
                achievements.takeIf { it.isNotEmpty() }?.let { appendLine("  ${it.joinToString("; ")}") }
            }.trimEnd()
        }
        return section("학력", blocks.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun certifications(items: List<CertificationAward>): String? {
        val blocks = items.mapNotNull { cert ->
            val name = cert.name.clean()
            val issuer = cert.issuer.clean()
            val date = cert.date.clean()
            if (name == null && issuer == null && date == null) return@mapNotNull null
            "• ${name.orEmpty()}${issuer?.let { " — $it" }.orEmpty()}${date?.let { " ($it)" }.orEmpty()}".trimEnd()
        }
        return section("자격증", blocks.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun awards(items: List<Award>): String? {
        val blocks = items.mapNotNull { award ->
            val title = award.title.clean()
            val org = award.organization.clean()
            val date = award.date.clean()
            if (title == null && org == null && date == null) return@mapNotNull null
            "• ${title.orEmpty()}${org?.let { " — $it" }.orEmpty()}${date?.let { " ($it)" }.orEmpty()}".trimEnd()
        }
        return section("수상", blocks.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun languages(langs: Map<String, String>): String? {
        val rows = langs.entries.mapNotNull { (key, value) ->
            val name = key.clean() ?: return@mapNotNull null
            value.clean()?.let { "$name: $it" } ?: name
        }
        return section("어학", rows.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    private fun publications(items: List<Publication>): String? {
        val blocks = items.mapNotNull { pub ->
            val title = pub.title.clean()
            val venue = pub.venue.clean()
            val date = pub.date.clean()
            if (title == null && venue == null && date == null) return@mapNotNull null
            "• ${title.orEmpty()}${venue?.let { " — $it" }.orEmpty()}${date?.let { " ($it)" }.orEmpty()}".trimEnd()
        }
        return section("출판/논문", blocks.takeIf { it.isNotEmpty() }?.joinToString("\n"))
    }

    /** Section with a `== title ==` header; returns null when the body is blank so empty sections vanish. */
    private fun section(title: String, body: String?): String? =
        body?.takeIf { it.isNotBlank() }?.let { "== $title ==\n${it.trimEnd()}" }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private fun List<String>.clean(): List<String> = mapNotNull { it.clean() }
}

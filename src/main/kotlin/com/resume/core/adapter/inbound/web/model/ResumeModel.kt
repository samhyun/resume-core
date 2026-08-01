package com.resume.core.adapter.inbound.web.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.resume.core.application.dto.write.SaveResumeCommand
import com.resume.core.application.dto.write.SaveResumeResult
import com.resume.core.application.dto.write.UpdateResumeCommand
import com.resume.core.application.dto.write.UpdateResumeResult
import com.resume.core.domain.model.AdditionalInfo
import com.resume.core.domain.model.Award
import com.resume.core.domain.model.CertificationAward
import com.resume.core.domain.model.Education
import com.resume.core.domain.model.Experience
import com.resume.core.domain.model.IndependentProject
import com.resume.core.domain.model.MilitaryService
import com.resume.core.domain.model.PortfolioItem
import com.resume.core.domain.model.Project
import com.resume.core.domain.model.Publication
import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeData
import com.resume.core.domain.model.ResumeLink
import com.resume.core.domain.model.ResumeMetadata
import com.resume.core.domain.model.ResumeSummary
import com.resume.core.domain.model.Skills
import com.resume.core.domain.model.VolunteerExperience
import java.time.LocalDateTime
import java.util.UUID

data class SaveResumeRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val summary: ResumeSummaryDto = ResumeSummaryDto(),
    val experience: List<ExperienceDto> = emptyList(),
    val skills: SkillsDto = SkillsDto(),
    val projects: List<IndependentProjectDto>? = null,
    val education: List<EducationDto> = emptyList(),
    @param:JsonProperty("certifications")
    @param:JsonAlias("certifications_awards")
    val certifications: List<CertificationAwardDto> = emptyList(),
    val awards: List<AwardDto> = emptyList(),
    val languages: Map<String, String> = emptyMap(),
    val volunteer: List<VolunteerDto> = emptyList(),
    @param:JsonProperty("personal_projects")
    val personalProjects: List<IndependentProjectDto> = emptyList(),
    val portfolio: List<PortfolioItemDto> = emptyList(),
    val publications: List<PublicationDto> = emptyList(),
    val metadata: ResumeMetadataDto? = null,
    @param:JsonProperty("additional_info")
    val additionalInfo: AdditionalInfoDto? = null,
    val military: MilitaryDto? = null
) {
    fun toCommand(userId: String): SaveResumeCommand =
        SaveResumeCommand(
            userId = userId,
            resumeData = toResumeData()
        )

    fun toUpdateCommand(resumeId: UUID, userId: String): UpdateResumeCommand =
        UpdateResumeCommand(
            resumeId = resumeId,
            userId = userId,
            resumeData = toResumeData()
        )

    private fun toResumeData(): ResumeData =
        ResumeData(
            name = name,
            email = email,
            phone = phone,
            address = address,
            summary = summary.toDomain(),
            experience = experience.map { it.toDomain() },
            skills = skills.toDomain(),
            projects = projects?.map { it.toDomain() },
            education = education.map { it.toDomain() },
            certificationsAwards = certifications.map { it.toDomain() },
            awards = awards.map { it.toDomain() },
            languages = languages,
            volunteer = volunteer.map { it.toDomain() },
            personalProjects = personalProjects.map { it.toDomain() },
            portfolio = portfolio.map { it.toDomain() },
            publications = publications.map { it.toDomain() },
            metadata = metadata?.toDomain(),
            additionalInfo = additionalInfo?.toDomain(),
            military = military?.toDomain()
        )
}

data class SaveResumeResponse(
    val resumeId: UUID,
    val userId: String,
    val version: Int
) {
    companion object {
        fun from(result: SaveResumeResult): SaveResumeResponse =
            SaveResumeResponse(
                resumeId = result.resumeId,
                userId = result.userId,
                version = result.version
            )
    }
}

data class UpdateResumeResponse(
    val resumeId: UUID,
    val userId: String,
    val version: Int
) {
    companion object {
        fun from(result: UpdateResumeResult): UpdateResumeResponse =
            UpdateResumeResponse(
                resumeId = result.resumeId,
                userId = result.userId,
                version = result.version
            )
    }
}

data class GetResumeResponse(
    val resumeId: UUID,
    val userId: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val summary: ResumeSummaryDto,
    val experience: List<ExperienceDto>,
    val skills: SkillsDto,
    val projects: List<IndependentProjectDto>? = null,
    val education: List<EducationDto>,
    @param:JsonProperty("certifications")
    val certifications: List<CertificationAwardDto> = emptyList(),
    val awards: List<AwardDto> = emptyList(),
    val languages: Map<String, String> = emptyMap(),
    val volunteer: List<VolunteerDto> = emptyList(),
    @param:JsonProperty("personal_projects")
    val personalProjects: List<IndependentProjectDto> = emptyList(),
    val portfolio: List<PortfolioItemDto> = emptyList(),
    val publications: List<PublicationDto> = emptyList(),
    val metadata: ResumeMetadataDto? = null,
    @param:JsonProperty("additional_info")
    val additionalInfo: AdditionalInfoDto? = null,
    val military: MilitaryDto? = null,
    val version: Int,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun from(resume: Resume): GetResumeResponse =
            GetResumeResponse(
                resumeId = resume.id,
                userId = resume.userId,
                name = resume.resumeData.name,
                email = resume.resumeData.email,
                phone = resume.resumeData.phone,
                address = resume.resumeData.address,
                summary = ResumeSummaryDto.from(resume.resumeData.summary),
                experience = resume.resumeData.experience.map { ExperienceDto.from(it) },
                skills = SkillsDto.from(resume.resumeData.skills),
                projects = resume.resumeData.projects?.map { IndependentProjectDto.from(it) },
                education = resume.resumeData.education.map { EducationDto.from(it) },
                certifications = resume.resumeData.certificationsAwards.map { CertificationAwardDto.from(it) },
                awards = resume.resumeData.awards.map { AwardDto.from(it) },
                languages = resume.resumeData.languages,
                volunteer = resume.resumeData.volunteer.map { VolunteerDto.from(it) },
                personalProjects = resume.resumeData.personalProjects.map { IndependentProjectDto.from(it) },
                portfolio = resume.resumeData.portfolio.map { PortfolioItemDto.from(it) },
                publications = resume.resumeData.publications.map { PublicationDto.from(it) },
                metadata = resume.resumeData.metadata?.let { ResumeMetadataDto.from(it) },
                additionalInfo = resume.resumeData.additionalInfo?.let { AdditionalInfoDto.from(it) },
                military = resume.resumeData.military?.let { MilitaryDto.from(it) },
                version = resume.version,
                isActive = resume.isActive,
                createdAt = resume.createdAt,
                updatedAt = resume.updatedAt
            )
    }
}

data class ResumeSummaryDto(
    val headline: String = "",
    val profile: List<String> = emptyList(),
    @param:JsonProperty("core_strengths")
    val coreStrengths: List<String> = emptyList()
) {
    fun toDomain(): ResumeSummary = ResumeSummary(headline, profile, coreStrengths)

    companion object {
        fun from(domain: ResumeSummary): ResumeSummaryDto =
            ResumeSummaryDto(domain.headline, domain.profile, domain.coreStrengths)
    }
}

data class ExperienceDto(
    val company: String,
    val position: String? = null,
    @param:JsonProperty("period")
    @param:JsonAlias("duration")
    val duration: String? = null,
    val summary: String? = null,
    val projects: List<ProjectDto> = emptyList(),
    val positions: List<String> = emptyList(),
    val department: String? = null
) {
    fun toDomain(): Experience =
        Experience(
            company = company,
            position = position ?: positions.firstOrNull(),
            duration = duration,
            summary = summary,
            projects = projects.map { it.toDomain() },
            positions = positions,
            department = department
        )

    companion object {
        fun from(domain: Experience): ExperienceDto =
            ExperienceDto(
                company = domain.company,
                position = domain.position,
                duration = domain.duration,
                summary = domain.summary,
                projects = domain.projects.map { ProjectDto.from(it) },
                positions = domain.positions,
                department = domain.department
            )
    }
}

data class ProjectDto(
    val name: String,
    @param:JsonProperty("period")
    val period: String? = null,
    val challenge: String? = null,
    val actions: List<String> = emptyList(),
    val results: List<String> = emptyList(),
    @param:JsonProperty("tech_stack")
    @param:JsonAlias("technologies")
    val technologies: List<String> = emptyList(),
    val links: ResumeLinkDto? = null,
    val role: String? = null,
    val description: String? = null,
    val achievements: String? = null
) {
    fun toDomain(): Project =
        Project(
            name = name,
            period = period,
            challenge = challenge,
            actions = actions,
            results = results,
            technologies = technologies,
            links = links?.toDomain(),
            role = role,
            description = description,
            achievements = achievements
        )

    companion object {
        fun from(domain: Project): ProjectDto =
            ProjectDto(
                name = domain.name,
                period = domain.period,
                challenge = domain.challenge,
                actions = domain.actions,
                results = domain.results,
                technologies = domain.technologies,
                links = domain.links?.let { ResumeLinkDto.from(it) },
                role = domain.role,
                description = domain.description,
                achievements = domain.achievements
            )
    }
}

data class IndependentProjectDto(
    val name: String,
    @param:JsonProperty("period")
    val period: String? = null,
    val type: String? = null,
    val description: String? = null,
    val details: List<String> = emptyList(),
    @param:JsonProperty("tech_stack")
    @param:JsonAlias("technologies")
    val technologies: List<String> = emptyList(),
    val links: ResumeLinkDto? = null,
    val role: String? = null,
    val achievements: String? = null
) {
    fun toDomain(): IndependentProject =
        IndependentProject(
            name = name,
            period = period,
            type = type,
            description = description,
            details = details,
            technologies = technologies,
            links = links?.toDomain(),
            role = role,
            achievements = achievements
        )

    companion object {
        fun from(domain: IndependentProject): IndependentProjectDto =
            IndependentProjectDto(
                name = domain.name,
                period = domain.period,
                type = domain.type,
                description = domain.description,
                details = domain.details,
                technologies = domain.technologies,
                links = domain.links?.let { ResumeLinkDto.from(it) },
                role = domain.role,
                achievements = domain.achievements
            )
    }
}

data class SkillsDto(
    val programming: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val databases: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val cloud: List<String> = emptyList(),
    @param:JsonProperty("soft")
    val soft: List<String> = emptyList(),
    val other: List<String> = emptyList(),
    @param:JsonProperty("languages")
    val languageProficiency: Map<String, String> = emptyMap(),
    @param:JsonProperty("language_list")
    val languageList: List<String> = emptyList(),
    val technical: TechnicalSkillsDto? = null
) {
    fun toDomain(): Skills {
        val resolvedProgramming = programming.ifEmpty { technical?.languages ?: emptyList() }
        val resolvedFrameworks = frameworks.ifEmpty { technical?.frameworks ?: emptyList() }
        val resolvedDatabases = databases.ifEmpty { technical?.databases ?: emptyList() }
        val resolvedTools = tools.ifEmpty { technical?.tools ?: emptyList() }
        val resolvedCloud = cloud.ifEmpty { technical?.cloud ?: emptyList() }
        val resolvedOther = other.ifEmpty { technical?.other ?: emptyList() }
        val spoken = languageProficiency
        val languageValues = if (languageList.isNotEmpty()) {
            languageList
        } else {
            spoken.map { (lang, level) -> "$lang ($level)" }
        }

        return Skills(
            programming = resolvedProgramming,
            frameworks = resolvedFrameworks,
            databases = resolvedDatabases,
            tools = resolvedTools,
            cloud = resolvedCloud,
            languages = languageValues,
            softSkills = soft,
            spokenLanguages = spoken,
            other = resolvedOther
        )
    }

    companion object {
        fun from(domain: Skills): SkillsDto =
            SkillsDto(
                programming = domain.programming,
                frameworks = domain.frameworks,
                databases = domain.databases,
                tools = domain.tools,
                cloud = domain.cloud,
                soft = domain.softSkills,
                other = domain.other,
                languageProficiency = domain.spokenLanguages,
                languageList = domain.languages,
                technical = TechnicalSkillsDto.from(domain)
            )
    }
}

data class TechnicalSkillsDto(
    val languages: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val databases: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val cloud: List<String> = emptyList(),
    val other: List<String> = emptyList()
) {
    companion object {
        fun from(domain: Skills): TechnicalSkillsDto =
            TechnicalSkillsDto(
                languages = domain.programming,
                frameworks = domain.frameworks,
                databases = domain.databases,
                tools = domain.tools,
                cloud = domain.cloud,
                other = domain.other
            )
    }
}

data class EducationDto(
    @param:JsonAlias("school")
    val institution: String,
    val degree: String? = null,
    val major: String? = null,
    @param:JsonProperty("graduation_year")
    val graduationYear: String? = null,
    val gpa: String? = null,
    val achievements: List<String>? = null,
    val notes: String? = null
) {
    fun toDomain(): Education = Education(institution, degree, major, graduationYear, gpa, achievements, notes)

    companion object {
        fun from(domain: Education): EducationDto =
            EducationDto(
                institution = domain.institution,
                degree = domain.degree,
                major = domain.major,
                graduationYear = domain.graduationYear,
                gpa = domain.gpa,
                achievements = domain.achievements,
                notes = domain.notes
            )
    }
}

data class CertificationAwardDto(
    val name: String,
    val issuer: String,
    val date: String,
    val expiry: String? = null
) {
    fun toDomain(): CertificationAward = CertificationAward(name, issuer, date, expiry)

    companion object {
        fun from(domain: CertificationAward): CertificationAwardDto =
            CertificationAwardDto(domain.name, domain.issuer, domain.date, domain.expiry)
    }
}

data class AwardDto(
    val title: String,
    val organization: String? = null,
    val date: String? = null,
    val description: String? = null
) {
    fun toDomain(): Award = Award(title, organization, date, description)

    companion object {
        fun from(domain: Award): AwardDto =
            AwardDto(domain.title, domain.organization, domain.date, domain.description)
    }
}

data class VolunteerDto(
    val name: String,
    val organization: String? = null,
    @param:JsonProperty("period")
    val period: String? = null,
    val description: String? = null,
    val role: String? = null
) {
    fun toDomain(): VolunteerExperience =
        VolunteerExperience(name = name, organization = organization, period = period, description = description, role = role)

    companion object {
        fun from(domain: VolunteerExperience): VolunteerDto =
            VolunteerDto(
                name = domain.name,
                organization = domain.organization,
                period = domain.period,
                description = domain.description,
                role = domain.role
            )
    }
}

data class MilitaryDto(
    val branch: String,
    val rank: String? = null,
    @param:JsonProperty("period")
    val period: String,
    @param:JsonProperty("discharge_type")
    val dischargeType: String? = null
) {
    fun toDomain(): MilitaryService = MilitaryService(branch, rank, period, dischargeType)

    companion object {
        fun from(domain: MilitaryService): MilitaryDto =
            MilitaryDto(domain.branch, domain.rank, domain.period, domain.dischargeType)
    }
}

data class PortfolioItemDto(
    val name: String? = null,
    val url: String? = null,
    val description: String? = null
) {
    fun toDomain(): PortfolioItem = PortfolioItem(name, url, description)

    companion object {
        fun from(domain: PortfolioItem): PortfolioItemDto =
            PortfolioItemDto(domain.name, domain.url, domain.description)
    }
}

data class PublicationDto(
    val title: String,
    val authors: List<String> = emptyList(),
    val venue: String? = null,
    val date: String? = null,
    val url: String? = null
) {
    fun toDomain(): Publication = Publication(title, authors, venue, date, url)

    companion object {
        fun from(domain: Publication): PublicationDto =
            PublicationDto(domain.title, domain.authors, domain.venue, domain.date, domain.url)
    }
}

data class ResumeMetadataDto(
    @param:JsonProperty("generated_at")
    val generatedAt: String? = null,
    val version: String? = null,
    val source: String? = null,
    val attempt: Int? = null
) {
    fun toDomain(): ResumeMetadata = ResumeMetadata(generatedAt, version, source, attempt)

    companion object {
        fun from(domain: ResumeMetadata): ResumeMetadataDto =
            ResumeMetadataDto(domain.generatedAt, domain.version, domain.source, domain.attempt)
    }
}

data class AdditionalInfoDto(
    val publications: List<String>? = null,
    val patents: List<String>? = null,
    @param:JsonProperty("speaking_activities")
    val speakingActivities: List<String>? = null,
    @param:JsonProperty("target_position")
    val targetPosition: String? = null
) {
    fun toDomain(): AdditionalInfo =
        AdditionalInfo(publications, patents, speakingActivities, targetPosition)

    companion object {
        fun from(domain: AdditionalInfo): AdditionalInfoDto =
            AdditionalInfoDto(
                domain.publications,
                domain.patents,
                domain.speakingActivities,
                domain.targetPosition
            )
    }
}

data class ResumeLinkDto(
    val demo: String? = null,
    val repo: String? = null,
    val github: String? = null,
    val website: String? = null
) {
    fun toDomain(): ResumeLink = ResumeLink(demo, repo, github, website)

    companion object {
        private const val GITHUB_HOST = "github.com"

        /** github.com 외에 저장소 URL로 볼 수 있는 호스트 목록. `repo` 슬롯 판별에 쓴다. */
        private val REPO_HOSTS = listOf("gitlab.com", "bitbucket.org", "gitee.com", "codeberg.org", "sourceforge.net")

        fun from(domain: ResumeLink): ResumeLinkDto =
            ResumeLinkDto(domain.demo, domain.repo, domain.github, domain.website)

        /**
         * URL 문자열에서 호스트만 잘라내 소문자로 돌려준다.
         *
         * `java.net.URI`는 스킴이 없는 `github.com/user/repo`에서 host를 null로 주고 형태가 깨진
         * 문자열에는 예외를 던진다. 배열로 들어온 링크는 애초에 규격 밖 입력이라 그런 값이 섞일 수
         * 있으므로, 파싱 대신 스킴(`://`) → 경로·쿼리·프래그먼트 → userinfo(`@`) → 포트(`:`) 순으로
         * 직접 떼어내고 선행 `www.`도 지운다. 어떤 입력에도 예외를 던지지 않는다.
         *
         * 잘라내기 전 `\`는 `/`로 정규화한다. 브라우저와 달리 그냥 두면 `https://evil.com\@github.com`
         * 처럼 authority 경계를 밀어 호스트를 속일 수 있어서다. userinfo 제거는 `://`가 있는 입력에만
         * 적용한다. 스킴 없는 정상 입력에는 `@`가 없고, `mailto:user@github.com` 같은 opaque URI가
         * 호스트로 둔갑하는 것을 막아야 한다.
         */
        private fun hostOf(url: String): String {
            val normalized = url.replace('\\', '/')
            val authority = normalized.substringAfter("://")
                .substringBefore('/')
                .substringBefore('?')
                .substringBefore('#')
            val hostPort = if (normalized.contains("://")) authority.substringAfterLast('@') else authority
            return hostPort.substringBefore(':').lowercase().removePrefix("www.")
        }

        /**
         * 호스트가 [domain] 자체이거나 그 하위 도메인인지 본다.
         *
         * 부분 문자열 비교라면 `github.com.evil.com`이나 `?next=github.com` 같은 값이
         * github으로 잡히므로 호스트 단위로만 판단한다.
         */
        private fun String.matchesHost(domain: String): Boolean =
            this == domain || endsWith(".$domain")

        /**
         * `http` / `https` 링크만 통과시키고 나머지는 null로 버린다.
         *
         * 여기 담긴 값은 프론트에서 `<a href>`로, PDF 렌더링에서는 템플릿으로 그대로 흘러가므로
         * `javascript:`, `data:`, `file:`, `vbscript:` 같은 스킴이 저장되면 저장형 XSS가 된다.
         * 허용 목록 방식으로 http·https만 남기고, 스킴을 판별할 수 없는 값도 함께 버린다.
         *
         * 판별 규칙:
         * - 경로·쿼리·프래그먼트 구분자보다 앞에 `:`가 없으면 스킴 없는 상대 URL로 보고 허용한다
         *   (`github.com/user/repo`처럼 스킴을 빼고 오는 입력이 실제로 들어온다).
         * - `:` 뒤가 숫자뿐이면 포트로 본다. `github.com:8443/x`가 스킴으로 오판되지 않게 한다.
         * - 그 외에는 `:` 앞을 스킴으로 보고 http·https일 때만 허용한다.
         *
         * 거부는 예외가 아니라 null이다. 링크 하나 때문에 이력서 저장 전체가 실패하지 않게 한다.
         */
        private fun String.takeIfSafeScheme(): String? {
            val boundary = indexOfFirst { it == '/' || it == '?' || it == '#' }.takeIf { it >= 0 } ?: length
            val colon = indexOf(':')
            if (colon < 0 || colon >= boundary) return this

            val afterColon = substring(colon + 1, boundary)
            if (afterColon.isNotEmpty() && afterColon.all { it.isDigit() }) return this

            val scheme = substring(0, colon)
            return takeIf { scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true) }
        }

        /**
         * links를 객체가 아닌 URL 배열로 보내오는 요청까지 함께 받는다.
         *
         * 정상 형태는 `{"github": "...", "demo": "..."}` 객체지만, AI 에이전트가 만든 JSON은
         * `[]` 나 `["https://github.com/..."]` 처럼 배열로 내려올 수 있다. 배열이면 URL 특성으로
         * 키를 추정하고, 비어 있으면 빈 링크로 둔다. 형태가 어긋난 요청 하나 때문에 이력서 저장
         * 전체가 실패하지 않게 하는 것이 목적이라, 어떤 입력에도 예외를 던지지 않는다.
         *
         * 배열·객체 어느 쪽이든 [takeIfSafeScheme]를 통과한 `http`/`https` 링크만 슬롯에 담는다.
         * `javascript:`나 `data:` 같은 값은 저장형 XSS 경로가 되므로 해당 슬롯을 null로 둔다.
         * 스킴이 없는 값은 지금까지처럼 상대 URL로 보고 그대로 받는다.
         *
         * 배열 분기의 분류 규칙:
         * - 문자열이 아닌 원소, 공백뿐인 문자열, 허용되지 않는 스킴, 중복 URL은 먼저 걸러낸다.
         * - 호스트가 `github.com`이거나 그 하위 도메인인 첫 URL → `github`
         * - 호스트가 [REPO_HOSTS]에 해당하는 첫 URL → `repo`. 없으면 남은 `github.com` URL이 `repo`로 간다.
         * - 남은 URL은 순서대로 `demo` → `website`에 채운다.
         *
         * 판별은 [hostOf]로 뽑은 호스트를 [matchesHost]로 비교하므로, `github.com.evil.com`이나
         * `?next=github.com` 같은 값은 github으로 잡히지 않는다. `github.io`도 별개 호스트라 제외된다.
         *
         * **한계**: 슬롯이 네 개뿐이라 걸러낸 뒤 URL이 다섯 개 이상이면 초과분은 담을 곳이 없어
         * 버려진다. 배열은 어차피 규격에서 벗어난 입력이므로 저장을 실패시키는 대신 이 손실을
         * 감수한다. 모든 링크를 보존해야 한다면 요청을 객체 형태로 보내야 한다.
         */
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromRaw(raw: Any?): ResumeLinkDto {
            fun String?.orNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

            if (raw is List<*>) {
                val remaining = raw.filterIsInstance<String>()
                    .mapNotNull { it.orNull()?.takeIfSafeScheme() }
                    .distinct()
                    .toMutableList()

                fun take(predicate: (String) -> Boolean): String? =
                    remaining.firstOrNull(predicate)?.also { remaining.remove(it) }

                fun isGithub(url: String): Boolean = hostOf(url).matchesHost(GITHUB_HOST)

                val github = take(::isGithub)
                val repo = take { url -> hostOf(url).let { host -> REPO_HOSTS.any { host.matchesHost(it) } } }
                    ?: take(::isGithub)

                return ResumeLinkDto(
                    demo = remaining.getOrNull(0),
                    repo = repo,
                    github = github,
                    website = remaining.getOrNull(1)
                )
            }

            if (raw is Map<*, *>) {
                fun value(key: String): String? = (raw[key] as? String).orNull()?.takeIfSafeScheme()
                return ResumeLinkDto(
                    demo = value("demo"),
                    repo = value("repo"),
                    github = value("github"),
                    website = value("website")
                )
            }

            return ResumeLinkDto()
        }
    }
}

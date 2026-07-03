package com.resume.core.adapter.inbound.web.model

import com.fasterxml.jackson.annotation.JsonAlias
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
        fun from(domain: ResumeLink): ResumeLinkDto =
            ResumeLinkDto(domain.demo, domain.repo, domain.github, domain.website)
    }
}

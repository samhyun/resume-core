package com.resume.core.adapter.inbound.web.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.resume.core.application.dto.write.SaveResumeCommand
import com.resume.core.application.dto.write.SaveResumeResult
import com.resume.core.application.dto.write.UpdateResumeCommand
import com.resume.core.application.dto.write.UpdateResumeResult
import com.resume.core.domain.model.*
import java.util.UUID

/**
 * Request model for saving a resume
 * Maps from frontend TypeScript FinalResume interface to domain model
 */
data class SaveResumeRequest(
    val summary: ResumeSummaryDto,
    val experience: List<ExperienceDto>,
    val skills: SkillsDto,
    val projects: List<IndependentProjectDto>? = null,
    val education: List<EducationDto>,
    @param:JsonProperty("certifications_awards")
    val certificationsAwards: List<CertificationAwardDto>? = null,
    @param:JsonProperty("additional_info")
    val additionalInfo: AdditionalInfoDto? = null
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
            summary = summary.toDomain(),
            experience = experience.map { it.toDomain() },
            skills = skills.toDomain(),
            projects = projects?.map { it.toDomain() },
            education = education.map { it.toDomain() },
            certificationsAwards = certificationsAwards?.map { it.toDomain() },
            additionalInfo = additionalInfo?.toDomain()
        )
}

/**
 * Response model for saved resume
 */
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

/**
 * Response model for updated resume
 */
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

/**
 * Response model for retrieved resume
 * Maps from domain model back to frontend TypeScript interface format
 */
data class GetResumeResponse(
    val resumeId: UUID,
    val userId: String,
    val summary: ResumeSummaryDto,
    val experience: List<ExperienceDto>,
    val skills: SkillsDto,
    val projects: List<IndependentProjectDto>? = null,
    val education: List<EducationDto>,
    @param:JsonProperty("certifications_awards")
    val certificationsAwards: List<CertificationAwardDto>? = null,
    @param:JsonProperty("additional_info")
    val additionalInfo: AdditionalInfoDto? = null,
    val version: Int,
    val isActive: Boolean
) {
    companion object {
        fun from(resume: Resume): GetResumeResponse =
            GetResumeResponse(
                resumeId = resume.id,
                userId = resume.userId,
                summary = ResumeSummaryDto.from(resume.resumeData.summary),
                experience = resume.resumeData.experience.map { ExperienceDto.from(it) },
                skills = SkillsDto.from(resume.resumeData.skills),
                projects = resume.resumeData.projects?.map { IndependentProjectDto.from(it) },
                education = resume.resumeData.education.map { EducationDto.from(it) },
                certificationsAwards = resume.resumeData.certificationsAwards?.map { CertificationAwardDto.from(it) },
                additionalInfo = resume.resumeData.additionalInfo?.let { AdditionalInfoDto.from(it) },
                version = resume.version,
                isActive = resume.isActive
            )
    }
}

// DTO classes matching frontend TypeScript interfaces

data class ResumeSummaryDto(
    val headline: String,
    val profile: List<String>,
    @param:JsonProperty("core_strengths")
    val coreStrengths: List<String>
) {
    fun toDomain(): ResumeSummary = ResumeSummary(headline, profile, coreStrengths)

    companion object {
        fun from(domain: ResumeSummary): ResumeSummaryDto =
            ResumeSummaryDto(domain.headline, domain.profile, domain.coreStrengths)
    }
}

data class ExperienceDto(
    val company: String,
    val position: String,
    val duration: String,
    val summary: String,
    val projects: List<ProjectDto>
) {
    fun toDomain(): Experience =
        Experience(company, position, duration, summary, projects.map { it.toDomain() })

    companion object {
        fun from(domain: Experience): ExperienceDto =
            ExperienceDto(
                domain.company,
                domain.position,
                domain.duration,
                domain.summary,
                domain.projects.map { ProjectDto.from(it) }
            )
    }
}

data class ProjectDto(
    val name: String,
    val period: String,
    val challenge: String,
    val actions: List<String>,
    val results: List<String>,
    val technologies: List<String>,
    val links: ResumeLinkDto? = null
) {
    fun toDomain(): Project = Project(name, period, challenge, actions, results, technologies, links?.toDomain())

    companion object {
        fun from(domain: Project): ProjectDto =
            ProjectDto(
                domain.name,
                domain.period,
                domain.challenge,
                domain.actions,
                domain.results,
                domain.technologies,
                domain.links?.let { ResumeLinkDto.from(it) }
            )
    }
}

data class IndependentProjectDto(
    val name: String,
    val period: String,
    val type: String,
    val description: String,
    val details: List<String>,
    val technologies: List<String>,
    val links: ResumeLinkDto? = null
) {
    fun toDomain(): IndependentProject =
        IndependentProject(name, period, type, description, details, technologies, links?.toDomain())

    companion object {
        fun from(domain: IndependentProject): IndependentProjectDto =
            IndependentProjectDto(
                domain.name,
                domain.period,
                domain.type,
                domain.description,
                domain.details,
                domain.technologies,
                domain.links?.let { ResumeLinkDto.from(it) }
            )
    }
}

data class SkillsDto(
    val programming: List<String>,
    val frameworks: List<String>,
    val databases: List<String>,
    val tools: List<String>,
    val cloud: List<String>,
    val languages: List<String>
) {
    fun toDomain(): Skills = Skills(programming, frameworks, databases, tools, cloud, languages)

    companion object {
        fun from(domain: Skills): SkillsDto =
            SkillsDto(
                domain.programming,
                domain.frameworks,
                domain.databases,
                domain.tools,
                domain.cloud,
                domain.languages
            )
    }
}

data class EducationDto(
    val institution: String,
    val degree: String,
    val major: String,
    @param:JsonProperty("graduation_year")
    val graduationYear: String,
    val gpa: String? = null,
    val achievements: List<String>? = null
) {
    fun toDomain(): Education = Education(institution, degree, major, graduationYear, gpa, achievements)

    companion object {
        fun from(domain: Education): EducationDto =
            EducationDto(
                domain.institution,
                domain.degree,
                domain.major,
                domain.graduationYear,
                domain.gpa,
                domain.achievements
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

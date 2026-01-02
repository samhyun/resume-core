package com.resume.core.domain.model

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

/**
 * Domain model representing a user's resume
 * Stores complete resume data in structured format
 */
data class Resume(
    val id: UUID,
    val userId: String,
    val resumeData: ResumeData,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Int,
    /** Indicates whether this resume is currently marked as the active one for the user */
    val isActive: Boolean
)

/**
 * Complete resume data structure matching frontend FinalResume interface
 */
data class ResumeData(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val summary: ResumeSummary = ResumeSummary(),
    val experience: List<Experience> = emptyList(),
    val skills: Skills = Skills(),
    val projects: List<IndependentProject>? = null,
    val education: List<Education> = emptyList(),
    @field:JsonProperty("certifications")
    @field:JsonAlias("certifications_awards")
    val certificationsAwards: List<CertificationAward> = emptyList(),
    val awards: List<Award> = emptyList(),
    val languages: Map<String, String> = emptyMap(),
    val volunteer: List<VolunteerExperience> = emptyList(),
    val military: MilitaryService? = null,
    @field:JsonProperty("personal_projects")
    val personalProjects: List<IndependentProject> = emptyList(),
    val portfolio: List<PortfolioItem> = emptyList(),
    val publications: List<Publication> = emptyList(),
    val metadata: ResumeMetadata? = null,
    val additionalInfo: AdditionalInfo? = null
)

data class ResumeSummary(
    val headline: String = "",
    val profile: List<String> = emptyList(),
    val coreStrengths: List<String> = emptyList()
)

data class Experience(
    val company: String,
    val position: String? = null,
    @field:JsonProperty("period")
    @field:JsonAlias("duration")
    val duration: String? = null,
    val summary: String? = null,
    val projects: List<Project> = emptyList(),
    val positions: List<String> = emptyList(),
    val department: String? = null
)

data class Project(
    val name: String,
    @field:JsonProperty("period")
    val period: String? = null,
    val challenge: String? = null,
    val actions: List<String> = emptyList(),
    val results: List<String> = emptyList(),
    @field:JsonAlias("tech_stack")
    val technologies: List<String> = emptyList(),
    val links: ResumeLink? = null,
    val role: String? = null,
    val description: String? = null,
    val achievements: String? = null
)

data class IndependentProject(
    val name: String,
    @field:JsonProperty("period")
    val period: String? = null,
    val type: String? = null,
    val description: String? = null,
    val details: List<String> = emptyList(),
    @field:JsonAlias("tech_stack")
    val technologies: List<String> = emptyList(),
    val links: ResumeLink? = null,
    val role: String? = null,
    val achievements: String? = null
)

data class Skills(
    val programming: List<String> = emptyList(),
    val frameworks: List<String> = emptyList(),
    val databases: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val cloud: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val softSkills: List<String> = emptyList(),
    val spokenLanguages: Map<String, String> = emptyMap(),
    val other: List<String> = emptyList()
)

data class Education(
    @field:JsonAlias("school")
    val institution: String,
    val degree: String? = null,
    val major: String? = null,
    @field:JsonProperty("graduation_year")
    val graduationYear: String? = null,
    val gpa: String? = null,
    val achievements: List<String>? = null,
    val notes: String? = null
)

data class CertificationAward(
    val name: String,
    val issuer: String,
    val date: String,
    val expiry: String? = null
)

data class AdditionalInfo(
    val publications: List<String>? = null,
    val patents: List<String>? = null,
    val speakingActivities: List<String>? = null,
    val targetPosition: String? = null
)

data class ResumeLink(
    val demo: String? = null,
    val repo: String? = null,
    val github: String? = null,
    val website: String? = null
)

data class ResumeMetadata(
    @field:JsonProperty("generated_at")
    val generatedAt: String? = null,
    val version: String? = null,
    val source: String? = null,
    val attempt: Int? = null
)

data class Award(
    val title: String,
    val organization: String? = null,
    val date: String? = null,
    val description: String? = null
)

data class VolunteerExperience(
    val name: String,
    val organization: String? = null,
    @field:JsonProperty("period")
    val period: String? = null,
    val description: String? = null,
    val role: String? = null
)

data class MilitaryService(
    val branch: String,
    val rank: String? = null,
    @field:JsonProperty("period")
    val period: String,
    @field:JsonProperty("discharge_type")
    val dischargeType: String? = null
)

data class PortfolioItem(
    val name: String? = null,
    val url: String? = null,
    val description: String? = null
)

data class Publication(
    val title: String,
    val authors: List<String> = emptyList(),
    val venue: String? = null,
    val date: String? = null,
    val url: String? = null
)

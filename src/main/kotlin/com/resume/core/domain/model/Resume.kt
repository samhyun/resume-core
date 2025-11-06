package com.resume.core.domain.model

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
    val summary: ResumeSummary,
    val experience: List<Experience>,
    val skills: Skills,
    val projects: List<IndependentProject>? = null,
    val education: List<Education>,
    val certificationsAwards: List<CertificationAward>? = null,
    val additionalInfo: AdditionalInfo? = null
)

data class ResumeSummary(
    val headline: String,
    val profile: List<String>,
    val coreStrengths: List<String>
)

data class Experience(
    val company: String,
    val position: String,
    val duration: String,
    val summary: String,
    val projects: List<Project>
)

data class Project(
    val name: String,
    val period: String,
    val challenge: String,
    val actions: List<String>,
    val results: List<String>,
    val technologies: List<String>,
    val links: ResumeLink? = null
)

data class IndependentProject(
    val name: String,
    val period: String,
    val type: String,
    val description: String,
    val details: List<String>,
    val technologies: List<String>,
    val links: ResumeLink? = null
)

data class Skills(
    val programming: List<String>,
    val frameworks: List<String>,
    val databases: List<String>,
    val tools: List<String>,
    val cloud: List<String>,
    val languages: List<String>
)

data class Education(
    val institution: String,
    val degree: String,
    val major: String,
    val graduationYear: String,
    val gpa: String? = null,
    val achievements: List<String>? = null
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

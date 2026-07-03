package com.resume.core.adapter.outbound.persistence.command.entity

import com.resume.core.domain.model.CoverLetter
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Entity for the cover_letter table. Maps domain [CoverLetter] to plain columns.
 */
@Table("cover_letter")
data class CoverLetterEntity(
    @Id
    @get:JvmName("getCoverLetterId")
    val id: UUID,

    @Column("user_id")
    val userId: String,

    @Column("resume_id")
    val resumeId: UUID?,

    @Column("company_name")
    val companyName: String,

    @Column("position")
    val position: String,

    @Column("job_description")
    val jobDescription: String?,

    @Column("content")
    val content: String,

    @Column("validation_score")
    val validationScore: Int?,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now(),

    @Column("version")
    val version: Int = 1
) : Persistable<UUID> {

    @Transient
    private var newRecord: Boolean = false

    override fun getId(): UUID = id

    override fun isNew(): Boolean = newRecord

    fun markNew(): CoverLetterEntity = apply { newRecord = true }

    fun markPersisted(): CoverLetterEntity = apply { newRecord = false }

    companion object {
        fun from(coverLetter: CoverLetter): CoverLetterEntity =
            CoverLetterEntity(
                id = coverLetter.id,
                userId = coverLetter.userId,
                resumeId = coverLetter.resumeId,
                companyName = coverLetter.companyName,
                position = coverLetter.position,
                jobDescription = coverLetter.jobDescription,
                content = coverLetter.content,
                validationScore = coverLetter.validationScore,
                createdAt = coverLetter.createdAt.toInstant(ZoneOffset.UTC),
                updatedAt = coverLetter.updatedAt.toInstant(ZoneOffset.UTC),
                version = coverLetter.version
            )
    }

    fun toDomain(): CoverLetter =
        CoverLetter(
            id = id,
            userId = userId,
            resumeId = resumeId,
            companyName = companyName,
            position = position,
            jobDescription = jobDescription,
            content = content,
            validationScore = validationScore,
            version = version,
            createdAt = LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC)
        )
}

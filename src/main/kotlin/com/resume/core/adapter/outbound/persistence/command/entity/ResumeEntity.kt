package com.resume.core.adapter.outbound.persistence.command.entity

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeData
import io.r2dbc.postgresql.codec.Json
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
 * Entity for resume table
 * Maps domain Resume model to database schema with JSONB storage
 */
@Table("resume")
data class ResumeEntity(
    @Id
    @get:JvmName("getResumeId")
    val id: UUID,

    @Column("user_id")
    val userId: String,

    @Column("resume_data")
    val resumeData: Json,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("updated_at")
    val updatedAt: Instant = Instant.now(),

    @Column("version")
    val version: Int = 1,

    @Column("is_active")
    val isActive: Boolean = true
) : Persistable<UUID> {

    @Transient
    private var newRecord: Boolean = false

    override fun getId(): UUID = id

    override fun isNew(): Boolean = newRecord

    fun markNew(): ResumeEntity = apply { newRecord = true }

    fun markPersisted(): ResumeEntity = apply { newRecord = false }

    companion object {
        private val objectMapper = jacksonObjectMapper()

        /**
         * Convert domain Resume to entity
         */
        fun from(resume: Resume): ResumeEntity {
            val jsonData = objectMapper.writeValueAsString(resume.resumeData)
            return ResumeEntity(
                id = resume.id,
                userId = resume.userId,
                resumeData = Json.of(jsonData),
                createdAt = resume.createdAt.toInstant(ZoneOffset.UTC),
                updatedAt = resume.updatedAt.toInstant(ZoneOffset.UTC),
                version = resume.version,
                isActive = resume.isActive
            )
        }
    }

    /**
     * Convert entity to domain Resume
     */
    fun toDomain(): Resume {
        val data: ResumeData = objectMapper.readValue(resumeData.asString())
        return Resume(
            id = id,
            userId = userId,
            resumeData = data,
            createdAt = LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC),
            version = version,
            isActive = isActive
        )
    }
}
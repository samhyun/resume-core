package com.resume.core.adapter.outbound.persistence.command.entity

import io.r2dbc.postgresql.codec.Json
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID


@Table("chat_session")
data class ChatSessionEntity(
    @Id
    @get:JvmName("getSessionId")
    val id: UUID,

    @Column("user_id")
    val userId: String,

    @Column("agent_session_id")
    val agentSessionId: String,

    @Column("app_name")
    val appName: String,

    @Column("state")
    val state: Json,

    val purpose: String?,

    val status: String,                  // "ACTIVE" or "CLOSED"

    @Column("last_update_time")
    val lastUpdateTime: Double?,

    @Column("created_at")
    val createdAt: Instant = Instant.now(),

    @Column("ended_at")
    val endedAt: Instant? = null
) : Persistable<UUID> {

    @Transient
    private var newRecord: Boolean = false

    override fun getId(): UUID = id

    override fun isNew(): Boolean = newRecord

    fun markNew(): ChatSessionEntity = apply { newRecord = true }

    fun markPersisted(): ChatSessionEntity = apply { newRecord = false }
}

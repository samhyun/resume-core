package com.resume.core.adapter.outbound.persistence.command.repository

import com.resume.core.config.R2dbcTxConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import io.r2dbc.postgresql.codec.Json
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@DataR2dbcTest
@Import(R2dbcTxConfig::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatSessionRepositoryTest @Autowired constructor(
    private val repository: ChatSessionRepository,
    private val databaseClient: DatabaseClient
) {

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("resume_test")
            withUsername("tester")
            withPassword("secret")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureR2dbc(registry: DynamicPropertyRegistry) {
            if (!postgres.isRunning) {
                postgres.start()
            }
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.enabled") { false }
        }
    }

    @BeforeEach
    fun cleanTable() {
        databaseClient.sql("DROP TABLE IF EXISTS chat_session").fetch().rowsUpdated().block()
        databaseClient.sql(
            """
            CREATE TABLE chat_session (
                id UUID PRIMARY KEY,
                user_id TEXT NOT NULL,
                agent_session_id TEXT NOT NULL UNIQUE,
                app_name TEXT NOT NULL,
                state JSONB NOT NULL,
                purpose TEXT,
                status TEXT NOT NULL,
                last_update_time DOUBLE PRECISION,
                created_at TIMESTAMPTZ NOT NULL,
                ended_at TIMESTAMPTZ
            )
            """
        ).fetch().rowsUpdated().block()
    }

    @Test
    fun `closeActiveByUser should update status to CLOSED`() {
        val id = UUID.randomUUID()
        insertSession(
            id = id,
            userId = "user-1",
            agentSessionId = "ext-1",
            status = "ACTIVE",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            lastUpdateTime = 1.0
        )

        repository.closeActiveByUser("user-1").block()

        val status = databaseClient.sql("SELECT status FROM chat_session WHERE user_id = :userId")
            .bind("userId", "user-1")
            .map { row, _ -> row.get("status", String::class.java) }
            .one()
            .block()

        assertThat(status).isEqualTo("CLOSED")
    }

    @Test
    fun `findActiveByUser should return most recent active session`() {
        val olderId = UUID.randomUUID()
        val newerId = UUID.randomUUID()
        val newerCreatedAt = Instant.parse("2024-02-01T00:00:00Z")

        insertSession(
            id = olderId,
            userId = "user-2",
            agentSessionId = "ext-old",
            status = "ACTIVE",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            lastUpdateTime = 1.0
        )

        insertSession(
            id = newerId,
            userId = "user-2",
            agentSessionId = "ext-new",
            status = "ACTIVE",
            createdAt = newerCreatedAt,
            lastUpdateTime = 2.0
        )

        val entity = repository.findActiveByUser("user-2").block()
        assertThat(entity).isNotNull
        assertThat(entity!!.agentSessionId).isEqualTo("ext-new")
    }

    private fun insertSession(
        id: UUID,
        userId: String,
        agentSessionId: String,
        status: String,
        createdAt: Instant,
        lastUpdateTime: Double? = null,
        purpose: String? = "general"
    ) {
        var spec = databaseClient.sql(
            """
            INSERT INTO chat_session (
                id, user_id, agent_session_id, app_name, state, purpose, status, last_update_time, created_at, ended_at
            ) VALUES (:id, :userId, :agentSessionId, :appName, :state, :purpose, :status, :lastUpdateTime, :createdAt, :endedAt)
            """
        )

        spec = spec.bind("id", id)
            .bind("userId", userId)
            .bind("agentSessionId", agentSessionId)
            .bind("appName", "resume-core")
            .bind("state", Json.of("{}"))

        spec = if (purpose != null) {
            spec.bind("purpose", purpose)
        } else {
            spec.bindNull("purpose", String::class.java)
        }

        spec = spec.bind("status", status)

        spec = if (lastUpdateTime != null) {
            spec.bind("lastUpdateTime", lastUpdateTime)
        } else {
            spec.bindNull("lastUpdateTime", Double::class.java)
        }

        spec = spec.bind("createdAt", OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
            .bindNull("endedAt", OffsetDateTime::class.java)

        spec.fetch()
            .rowsUpdated()
            .block()
    }
}

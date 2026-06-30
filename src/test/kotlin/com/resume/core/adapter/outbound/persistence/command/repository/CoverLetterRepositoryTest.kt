package com.resume.core.adapter.outbound.persistence.command.repository

import com.resume.core.adapter.outbound.persistence.command.entity.CoverLetterEntity
import com.resume.core.config.R2dbcTxConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

@DataR2dbcTest
@Import(R2dbcTxConfig::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoverLetterRepositoryTest @Autowired constructor(
    private val repository: CoverLetterRepository,
    private val databaseClient: DatabaseClient
) {

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
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
                "r2dbc:postgresql://${postgres.host}:${postgres.firstMappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }
            registry.add("spring.flyway.enabled") { false }
        }
    }

    @BeforeEach
    fun cleanTable() {
        databaseClient.sql("DROP TABLE IF EXISTS cover_letter").fetch().rowsUpdated().block()
        databaseClient.sql(
            """
            CREATE TABLE cover_letter (
                id UUID PRIMARY KEY,
                user_id VARCHAR(255) NOT NULL,
                resume_id UUID,
                company_name VARCHAR(255) NOT NULL,
                position VARCHAR(255) NOT NULL,
                job_description TEXT,
                content TEXT NOT NULL,
                validation_score INTEGER,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                version INTEGER NOT NULL DEFAULT 1
            )
            """
        ).fetch().rowsUpdated().block()
        databaseClient.sql(
            """
            CREATE OR REPLACE FUNCTION update_cover_letter_updated_at()
            RETURNS TRIGGER AS ${'$'}${'$'}
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                NEW.version = OLD.version + 1;
                RETURN NEW;
            END;
            ${'$'}${'$'} LANGUAGE plpgsql
            """
        ).fetch().rowsUpdated().block()
        databaseClient.sql("DROP TRIGGER IF EXISTS trigger_cover_letter_updated_at ON cover_letter")
            .fetch().rowsUpdated().block()
        databaseClient.sql(
            """
            CREATE TRIGGER trigger_cover_letter_updated_at
                BEFORE UPDATE ON cover_letter
                FOR EACH ROW
                EXECUTE FUNCTION update_cover_letter_updated_at()
            """
        ).fetch().rowsUpdated().block()
    }

    @Test
    fun `save then find by id and user returns the cover letter including position column`() {
        val id = UUID.randomUUID()
        repository.save(entity(id, "user-1", company = "Acme", position = "Backend Engineer")).block()

        val found = repository.findByIdAndUserId(id, "user-1").block()

        assertThat(found).isNotNull
        assertThat(found!!.companyName).isEqualTo("Acme")
        assertThat(found.position).isEqualTo("Backend Engineer")
        assertThat(found.content).isEqualTo("본문")
        assertThat(found.version).isEqualTo(1)
    }

    @Test
    fun `findByIdAndUserId is scoped to the owner`() {
        val id = UUID.randomUUID()
        repository.save(entity(id, "owner")).block()

        assertThat(repository.findByIdAndUserId(id, "intruder").block()).isNull()
        assertThat(repository.existsByIdAndUserId(id, "intruder").block()).isFalse()
    }

    @Test
    fun `findAllByUserId returns newest first`() {
        val older = UUID.randomUUID()
        val newer = UUID.randomUUID()
        repository.save(entity(older, "user-2", company = "Older")).block()
        repository.save(entity(newer, "user-2", company = "Newer")).block()
        // bump created_at so ordering is deterministic
        databaseClient.sql("UPDATE cover_letter SET created_at = created_at + interval '1 hour' WHERE id = :id")
            .bind("id", newer).fetch().rowsUpdated().block()

        val all = repository.findAllByUserId("user-2").collectList().block()

        assertThat(all).hasSize(2)
        assertThat(all!![0].companyName).isEqualTo("Newer")
    }

    @Test
    fun `update bumps version via trigger`() {
        val id = UUID.randomUUID()
        repository.save(entity(id, "user-1", content = "first")).block()

        val updated = entity(id, "user-1", content = "edited").markPersisted() // isNew=false → UPDATE
        repository.save(updated).block()

        val found = repository.findByIdAndUserId(id, "user-1").block()
        assertThat(found!!.content).isEqualTo("edited")
        assertThat(found.version).isEqualTo(2)
    }

    @Test
    fun `delete by id and user removes the row`() {
        val id = UUID.randomUUID()
        repository.save(entity(id, "user-1")).block()

        repository.deleteByIdAndUserId(id, "user-1").block()

        assertThat(repository.findByIdAndUserId(id, "user-1").block()).isNull()
    }

    private fun entity(
        id: UUID,
        userId: String,
        company: String = "Acme",
        position: String = "Backend Engineer",
        content: String = "본문"
    ): CoverLetterEntity =
        CoverLetterEntity(
            id = id,
            userId = userId,
            resumeId = null,
            companyName = company,
            position = position,
            jobDescription = null,
            content = content,
            validationScore = 80
        ).markNew()
}

package com.resume.core.adapter.outbound.persistence.command.repository

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
class AppUserRepositoryTest @Autowired constructor(
    private val repository: AppUserRepository,
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
        databaseClient.sql("DROP TABLE IF EXISTS app_user").fetch().rowsUpdated().block()
        databaseClient.sql(
            """
            CREATE TABLE app_user (
                id UUID PRIMARY KEY,
                username VARCHAR(100) NOT NULL,
                email VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                enabled BOOLEAN NOT NULL DEFAULT true,
                email_verified BOOLEAN NOT NULL DEFAULT false,
                display_name VARCHAR(100),
                first_name VARCHAR(100),
                last_name VARCHAR(100),
                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
            )
            """
        ).fetch().rowsUpdated().block()
    }

    @Test
    fun `findByUsername matches case-insensitively`() {
        insertUser(username = "Tester", email = "t@e.com", passwordHash = "hash", displayName = "old", emailVerified = true)

        val found = repository.findByUsername("tester").block()

        assertThat(found).isNotNull
        assertThat(found!!.username).isEqualTo("Tester")
        assertThat(found.displayName).isEqualTo("old")
        assertThat(found.emailVerified).isTrue()
    }

    @Test
    fun `updateProfile changes only display fields, leaving password and email intact`() {
        insertUser(username = "tester", email = "t@e.com", passwordHash = "secret-hash", displayName = "old")

        val updated = repository.updateProfile("TESTER", "새 표시명", "길동", "홍").block()

        assertThat(updated).isNotNull
        assertThat(updated!!.displayName).isEqualTo("새 표시명")
        assertThat(updated.firstName).isEqualTo("길동")
        assertThat(updated.lastName).isEqualTo("홍")
        assertThat(updated.email).isEqualTo("t@e.com") // RETURNING 으로 함께 반환, 변경 안 됨

        // 타깃 UPDATE 가 password_hash 를 건드리지 않았는지 직접 확인
        val raw = databaseClient.sql("SELECT password_hash FROM app_user WHERE LOWER(username) = 'tester'")
            .fetch().one().block()!!
        assertThat(raw["password_hash"]).isEqualTo("secret-hash")
    }

    @Test
    fun `updateProfile clears fields when given null`() {
        insertUser(username = "tester", email = "t@e.com", passwordHash = "hash", displayName = "old", firstName = "x")

        val updated = repository.updateProfile("tester", null, null, null).block()!!

        assertThat(updated.displayName).isNull()
        assertThat(updated.firstName).isNull()
    }

    private fun insertUser(
        username: String,
        email: String,
        passwordHash: String,
        displayName: String? = null,
        firstName: String? = null,
        lastName: String? = null,
        emailVerified: Boolean = false
    ) {
        var spec = databaseClient.sql(
            """
            INSERT INTO app_user (id, username, email, password_hash, email_verified, display_name, first_name, last_name)
            VALUES (:id, :username, :email, :passwordHash, :emailVerified, :displayName, :firstName, :lastName)
            """
        )
            .bind("id", UUID.randomUUID())
            .bind("username", username)
            .bind("email", email)
            .bind("passwordHash", passwordHash)
            .bind("emailVerified", emailVerified)

        spec = if (displayName != null) spec.bind("displayName", displayName) else spec.bindNull("displayName", String::class.java)
        spec = if (firstName != null) spec.bind("firstName", firstName) else spec.bindNull("firstName", String::class.java)
        spec = if (lastName != null) spec.bind("lastName", lastName) else spec.bindNull("lastName", String::class.java)

        spec.fetch().rowsUpdated().block()
    }
}

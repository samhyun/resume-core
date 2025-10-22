package com.resume.core.adapter.outbound.persistence.command.repository

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.resume.core.config.R2dbcTxConfig
import com.resume.core.domain.model.*
import io.r2dbc.postgresql.codec.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * ResumeRepository 통합 테스트
 * Testcontainers를 사용하여 실제 PostgreSQL 환경에서 테스트
 */
@DataR2dbcTest
@Import(R2dbcTxConfig::class)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResumeRepositoryTest @Autowired constructor(
    private val repository: ResumeRepository,
    private val databaseClient: DatabaseClient
) {

    private val objectMapper = jacksonObjectMapper()

    companion object {
        /**
         * 테스트용 PostgreSQL 컨테이너
         * 로컬 환경(5432)과 충돌하지 않도록 동적 포트 할당
         */
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("resume_test")
            withUsername("test_user")
            withPassword("test_password")
            // Testcontainers가 자동으로 사용 가능한 포트를 할당합니다
        }

        /**
         * R2DBC 설정을 Testcontainers의 동적 포트로 설정
         */
        @JvmStatic
        @DynamicPropertySource
        fun configureR2dbc(registry: DynamicPropertyRegistry) {
            if (!postgres.isRunning) {
                postgres.start()
            }

            // Testcontainers가 할당한 동적 포트 사용
            val mappedPort = postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${mappedPort}/${postgres.databaseName}"
            }
            registry.add("spring.r2dbc.username") { postgres.username }
            registry.add("spring.r2dbc.password") { postgres.password }

            // Flyway는 테스트에서 비활성화 (스키마는 직접 생성)
            registry.add("spring.flyway.enabled") { false }
        }
    }

    /**
     * 각 테스트 전에 테이블을 새로 생성하여 격리된 환경 보장
     */
    @BeforeEach
    fun setupTable() {
        // 기존 테이블 삭제
        databaseClient.sql("DROP TABLE IF EXISTS resume CASCADE").fetch().rowsUpdated().block()

        // 테스트용 스키마 생성 (V3_0_0 + V3_0_1 적용된 상태)
        databaseClient.sql(
            """
            CREATE TABLE resume (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                user_id VARCHAR(255) NOT NULL,
                resume_data JSONB NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                version INTEGER NOT NULL DEFAULT 1,
                is_active BOOLEAN NOT NULL DEFAULT TRUE
            )
            """
        ).fetch().rowsUpdated().block()

        // 인덱스 생성
        databaseClient.sql(
            "CREATE INDEX idx_resume_user_id ON resume(user_id)"
        ).fetch().rowsUpdated().block()

        databaseClient.sql(
            "CREATE INDEX idx_resume_user_active ON resume(user_id, is_active) WHERE is_active = TRUE"
        ).fetch().rowsUpdated().block()

        databaseClient.sql(
            "CREATE INDEX idx_resume_data_gin ON resume USING GIN (resume_data)"
        ).fetch().rowsUpdated().block()
    }

    @Test
    fun `findByIdAndUserId should return resume when exists`() {
        // Given
        val resumeId = UUID.randomUUID()
        val userId = "user-123"
        val resumeData = createSampleResumeData()

        insertResume(
            id = resumeId,
            userId = userId,
            resumeData = resumeData,
            isActive = true
        )

        // When
        val found = repository.findByIdAndUserId(resumeId, userId).block()

        // Then
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(resumeId)
        assertThat(found.userId).isEqualTo(userId)
        assertThat(found.isActive).isTrue()
    }

    @Test
    fun `findByIdAndUserId should return empty when user does not match`() {
        // Given
        val resumeId = UUID.randomUUID()
        val resumeData = createSampleResumeData()

        insertResume(
            id = resumeId,
            userId = "user-123",
            resumeData = resumeData,
            isActive = true
        )

        // When
        val found = repository.findByIdAndUserId(resumeId, "other-user").block()

        // Then
        assertThat(found).isNull()
    }

    @Test
    fun `findActiveByUserId should return most recent active resume`() {
        // Given
        val userId = "user-456"
        val olderResume = UUID.randomUUID()
        val newerResume = UUID.randomUUID()
        val resumeData = createSampleResumeData()

        insertResume(
            id = olderResume,
            userId = userId,
            resumeData = resumeData,
            isActive = true,
            createdAt = Instant.parse("2024-01-01T00:00:00Z")
        )

        insertResume(
            id = newerResume,
            userId = userId,
            resumeData = resumeData,
            isActive = true,
            createdAt = Instant.parse("2024-02-01T00:00:00Z")
        )

        // When
        val found = repository.findActiveByUserId(userId).block()

        // Then
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(newerResume)
    }

    @Test
    fun `findActiveByUserId should not return inactive resumes`() {
        // Given
        val userId = "user-789"
        val resumeId = UUID.randomUUID()
        val resumeData = createSampleResumeData()

        insertResume(
            id = resumeId,
            userId = userId,
            resumeData = resumeData,
            isActive = false
        )

        // When
        val found = repository.findActiveByUserId(userId).block()

        // Then
        assertThat(found).isNull()
    }

    @Test
    fun `deactivateAllByUserId should set all active resumes to inactive`() {
        // Given
        val userId = "user-999"
        val resume1 = UUID.randomUUID()
        val resume2 = UUID.randomUUID()
        val resumeData = createSampleResumeData()

        insertResume(id = resume1, userId = userId, resumeData = resumeData, isActive = true)
        insertResume(id = resume2, userId = userId, resumeData = resumeData, isActive = true)

        // When
        repository.deactivateAllByUserId(userId).block()

        // Then
        val activeCount = databaseClient.sql(
            "SELECT COUNT(*) FROM resume WHERE user_id = :userId AND is_active = true"
        )
            .bind("userId", userId)
            .map { row, _ -> row.get(0, Long::class.java) }
            .one()
            .block()

        assertThat(activeCount).isEqualTo(0L)

        // 비활성화된 이력서 확인
        val inactiveCount = databaseClient.sql(
            "SELECT COUNT(*) FROM resume WHERE user_id = :userId AND is_active = false"
        )
            .bind("userId", userId)
            .map { row, _ -> row.get(0, Long::class.java) }
            .one()
            .block()

        assertThat(inactiveCount).isEqualTo(2L)
    }

    @Test
    fun `existsByIdAndUserId should return true when resume exists`() {
        // Given
        val resumeId = UUID.randomUUID()
        val userId = "user-exists"
        val resumeData = createSampleResumeData()

        insertResume(id = resumeId, userId = userId, resumeData = resumeData, isActive = true)

        // When
        val exists = repository.existsByIdAndUserId(resumeId, userId).block()

        // Then
        assertThat(exists).isTrue()
    }

    @Test
    fun `existsByIdAndUserId should return false when user does not match`() {
        // Given
        val resumeId = UUID.randomUUID()
        val resumeData = createSampleResumeData()

        insertResume(id = resumeId, userId = "user-123", resumeData = resumeData, isActive = true)

        // When
        val exists = repository.existsByIdAndUserId(resumeId, "other-user").block()

        // Then
        assertThat(exists).isFalse()
    }

    /**
     * 테스트용 이력서 데이터 생성
     */
    private fun createSampleResumeData(): ResumeData {
        return ResumeData(
            summary = ResumeSummary(
                headline = "Test Developer",
                profile = listOf("Profile description"),
                coreStrengths = listOf("Kotlin", "Spring")
            ),
            experience = listOf(
                Experience(
                    company = "Test Corp",
                    position = "Developer",
                    duration = "2020-2024",
                    summary = "Development work",
                    projects = listOf(
                        Project(
                            name = "Test Project",
                            period = "2020-2021",
                            challenge = "Test challenge",
                            actions = listOf("Action 1"),
                            results = listOf("Result 1"),
                            technologies = listOf("Kotlin")
                        )
                    )
                )
            ),
            skills = Skills(
                programming = listOf("Kotlin"),
                frameworks = listOf("Spring"),
                databases = listOf("PostgreSQL"),
                tools = listOf("Git"),
                cloud = listOf("AWS"),
                languages = listOf("Korean")
            ),
            education = listOf(
                Education(
                    institution = "Test University",
                    degree = "Bachelor",
                    major = "Computer Science",
                    graduationYear = "2020"
                )
            )
        )
    }

    /**
     * 테스트용 이력서 직접 삽입 헬퍼 메서드
     */
    private fun insertResume(
        id: UUID,
        userId: String,
        resumeData: ResumeData,
        isActive: Boolean,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        version: Int = 1
    ) {
        val jsonData = objectMapper.writeValueAsString(resumeData)

        databaseClient.sql(
            """
            INSERT INTO resume (
                id, user_id, resume_data, created_at, updated_at, version, is_active
            ) VALUES (
                :id, :userId, :resumeData, :createdAt, :updatedAt, :version, :isActive
            )
            """
        )
            .bind("id", id)
            .bind("userId", userId)
            .bind("resumeData", Json.of(jsonData))
            .bind("createdAt", OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
            .bind("updatedAt", OffsetDateTime.ofInstant(updatedAt, ZoneOffset.UTC))
            .bind("version", version)
            .bind("isActive", isActive)
            .fetch()
            .rowsUpdated()
            .block()
    }
}
package com.resume.core.adapter.outbound.persistence.command.repository

import com.resume.core.adapter.outbound.persistence.command.entity.AppUserEntity
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * R2DBC access to `app_user`. Lookups are by username (case-insensitive, matching the
 * `LOWER(username)` unique index), since the JWT identifies the user via `preferred_username`.
 */
@Repository
interface AppUserRepository : ReactiveCrudRepository<AppUserEntity, UUID> {

    @Query(
        """
        SELECT id, username, email, display_name, first_name, last_name, email_verified
        FROM app_user
        WHERE LOWER(username) = LOWER(:username)
        """
    )
    fun findByUsername(username: String): Mono<AppUserEntity>

    /**
     * Update only the editable display fields. Identity columns (password_hash, email, enabled,
     * email_verified) are intentionally never touched. Uses `RETURNING` so the update and the read
     * of the fresh row are one atomic round-trip; empty when no such user. (`updated_at` is bumped by
     * the table's `trg_app_user_set_timestamp` trigger.)
     */
    @Query(
        """
        UPDATE app_user
        SET display_name = :displayName, first_name = :firstName, last_name = :lastName
        WHERE LOWER(username) = LOWER(:username)
        RETURNING id, username, email, display_name, first_name, last_name, email_verified
        """
    )
    fun updateProfile(
        username: String,
        displayName: String?,
        firstName: String?,
        lastName: String?
    ): Mono<AppUserEntity>
}

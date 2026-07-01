package com.resume.core.adapter.outbound.persistence.command.entity

import com.resume.core.domain.model.UserProfile
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

/**
 * Read view of the `app_user` table (the Keycloak-shared user store). Only the columns the
 * profile feature reads are mapped; identity columns (password_hash, enabled, ...) are left out
 * so profile updates can never touch them.
 */
@Table("app_user")
data class AppUserEntity(
    @Id
    @get:JvmName("getAppUserId")
    val id: UUID,

    val username: String,

    val email: String,

    @Column("display_name")
    val displayName: String?,

    @Column("first_name")
    val firstName: String?,

    @Column("last_name")
    val lastName: String?,

    @Column("email_verified")
    val emailVerified: Boolean
) {
    fun toDomain(): UserProfile =
        UserProfile(
            username = username,
            email = email,
            displayName = displayName,
            firstName = firstName,
            lastName = lastName,
            emailVerified = emailVerified
        )
}

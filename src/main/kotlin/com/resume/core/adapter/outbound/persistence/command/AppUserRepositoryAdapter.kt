package com.resume.core.adapter.outbound.persistence.command

import com.resume.core.adapter.outbound.persistence.command.repository.AppUserRepository
import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.persistence.UserProfileRepositoryPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class AppUserRepositoryAdapter(
    private val repository: AppUserRepository
) : UserProfileRepositoryPort {

    override fun findByUsername(username: String): Mono<UserProfile> =
        repository.findByUsername(username).map { it.toDomain() }

    override fun updateProfile(
        username: String,
        displayName: String?,
        firstName: String?,
        lastName: String?
    ): Mono<UserProfile> =
        repository.updateProfile(username, displayName, firstName, lastName)
            .map { it.toDomain() }
}

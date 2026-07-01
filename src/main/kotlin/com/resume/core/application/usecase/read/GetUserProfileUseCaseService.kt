package com.resume.core.application.usecase.read

import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.persistence.UserProfileRepositoryPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@Service
class GetUserProfileUseCaseService(
    private val userProfileRepository: UserProfileRepositoryPort
) : GetUserProfileUseCase {

    override fun handle(username: String): Mono<UserProfile> =
        userProfileRepository.findByUsername(username)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"))
            )
}

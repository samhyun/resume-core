package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.persistence.UserProfileRepositoryPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@Service
class UpdateUserProfileUseCaseService(
    private val userProfileRepository: UserProfileRepositoryPort
) : UpdateUserProfileUseCase {

    override fun handle(command: UpdateUserProfileCommand): Mono<UserProfile> =
        userProfileRepository.updateProfile(
            username = command.username,
            displayName = command.displayName,
            firstName = command.firstName,
            lastName = command.lastName
        ).switchIfEmpty(
            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"))
        )
}

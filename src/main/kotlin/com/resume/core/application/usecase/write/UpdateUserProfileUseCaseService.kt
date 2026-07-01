package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.external.UserProfilePort
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UpdateUserProfileUseCaseService(
    private val userProfilePort: UserProfilePort
) : UpdateUserProfileUseCase {

    override fun handle(accessToken: String, command: UpdateUserProfileCommand): Mono<UserProfile> =
        userProfilePort.updateProfile(accessToken, command.firstName, command.lastName)
}

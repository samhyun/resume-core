package com.resume.core.application.usecase.read

import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.external.UserProfilePort
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class GetUserProfileUseCaseService(
    private val userProfilePort: UserProfilePort
) : GetUserProfileUseCase {

    override fun handle(accessToken: String): Mono<UserProfile> =
        userProfilePort.getProfile(accessToken)
}

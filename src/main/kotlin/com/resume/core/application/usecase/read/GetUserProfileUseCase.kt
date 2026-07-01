package com.resume.core.application.usecase.read

import com.resume.core.domain.model.UserProfile
import reactor.core.publisher.Mono

interface GetUserProfileUseCase {
    fun handle(username: String): Mono<UserProfile>
}

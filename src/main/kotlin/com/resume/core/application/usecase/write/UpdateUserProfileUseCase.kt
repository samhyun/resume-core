package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.UpdateUserProfileCommand
import com.resume.core.domain.model.UserProfile
import reactor.core.publisher.Mono

interface UpdateUserProfileUseCase {
    fun handle(command: UpdateUserProfileCommand): Mono<UserProfile>
}

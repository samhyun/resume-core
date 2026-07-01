package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.model.ProfileResponse
import com.resume.core.adapter.inbound.web.model.UpdateProfileRequest
import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.application.usecase.read.GetUserProfileUseCase
import com.resume.core.application.usecase.write.UpdateUserProfileUseCase
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * The authenticated user's own profile. Always scoped to the JWT's `preferred_username` —
 * there is no path/userId parameter, so a user can only read/edit their own profile.
 */
@RestController
@RequestMapping("/api/resume-core/profile")
class ProfileController(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val authenticationFacade: ReactiveJwtAuthenticationFacade
) {

    @GetMapping
    fun get(): Mono<ProfileResponse> =
        authenticationFacade.currentUsername()
            .flatMap { username -> getUserProfileUseCase.handle(username) }
            .map(ProfileResponse::from)

    @PutMapping
    fun update(@RequestBody request: UpdateProfileRequest): Mono<ProfileResponse> =
        authenticationFacade.currentUsername()
            .flatMap { username -> updateUserProfileUseCase.handle(request.toCommand(username)) }
            .map(ProfileResponse::from)
}

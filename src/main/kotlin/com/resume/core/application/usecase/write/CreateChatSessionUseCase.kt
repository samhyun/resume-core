package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.CreateChatSessionResult
import com.resume.core.application.dto.write.CreateSessionCommand
import reactor.core.publisher.Mono

fun interface CreateChatSessionUseCase {
    fun handle(command: CreateSessionCommand): Mono<CreateChatSessionResult>
}


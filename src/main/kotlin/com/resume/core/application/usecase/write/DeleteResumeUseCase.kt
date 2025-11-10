package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.DeleteResumeCommand
import reactor.core.publisher.Mono

fun interface DeleteResumeUseCase {
    fun handle(command: DeleteResumeCommand): Mono<Unit>
}

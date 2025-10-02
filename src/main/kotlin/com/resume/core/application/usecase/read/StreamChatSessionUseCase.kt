package com.resume.core.application.usecase.read

import com.resume.core.application.dto.write.RunChatSessionCommand
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import reactor.core.publisher.Flux

fun interface StreamChatSessionUseCase {
    fun stream(command: RunChatSessionCommand): Flux<AiAgentStreamEvent>
}

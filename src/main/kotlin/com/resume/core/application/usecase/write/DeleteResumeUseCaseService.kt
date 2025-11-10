package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.DeleteResumeCommand
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@Service
class DeleteResumeUseCaseService(
    private val resumeRepository: ResumeRepositoryPort
) : DeleteResumeUseCase {

    override fun handle(command: DeleteResumeCommand): Mono<Unit> =
        resumeRepository.existsByIdAndUserId(command.resumeId, command.userId)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error<Unit>(
                        ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Resume not found"
                        )
                    )
                } else {
                    resumeRepository.deleteByIdAndUserId(command.resumeId, command.userId)
                }
            }
}

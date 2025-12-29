package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.UpdateResumeCommand
import com.resume.core.application.dto.write.UpdateResumeResult
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.LocalDateTime

/**
 * Service implementing UpdateResumeUseCase
 * Validates ownership and persists updated resume data
 */
@Service
class UpdateResumeUseCaseService(
    private val resumeRepository: ResumeRepositoryPort
) : UpdateResumeUseCase {

    override fun handle(command: UpdateResumeCommand): Mono<UpdateResumeResult> {
        val now = LocalDateTime.now()

        return resumeRepository.findByIdAndUserId(command.resumeId, command.userId)
            .switchIfEmpty(
                Mono.error(
                    ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not found")
                )
            )
            .flatMap { existing ->
                val updated = existing.copy(
                    resumeData = command.resumeData,
                    updatedAt = now,
                    version = existing.version + 1
                )
                resumeRepository.update(updated)
            }
            .map { saved ->
                UpdateResumeResult(
                    resumeId = saved.id,
                    userId = saved.userId,
                    version = saved.version
                )
            }
    }
}

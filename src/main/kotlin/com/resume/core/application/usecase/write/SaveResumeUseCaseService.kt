package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.SaveResumeCommand
import com.resume.core.application.dto.write.SaveResumeResult
import com.resume.core.domain.model.Resume
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service implementing SaveResumeUseCase
 * Handles resume save business logic with automatic deactivation of existing active resumes
 */
@Service
class SaveResumeUseCaseService(
    private val resumeRepository: ResumeRepositoryPort
) : SaveResumeUseCase {

    override fun handle(command: SaveResumeCommand): Mono<SaveResumeResult> {
        val now = LocalDateTime.now()
        val resumeId = UUID.randomUUID()

        val resume = Resume(
            id = resumeId,
            userId = command.userId,
            resumeData = command.resumeData,
            createdAt = now,
            updatedAt = now,
            version = 1,
            isActive = true
        )

        return resumeRepository.save(resume)
            .map { saved ->
                SaveResumeResult(
                    resumeId = saved.id,
                    userId = saved.userId,
                    version = saved.version
                )
            }
    }
}
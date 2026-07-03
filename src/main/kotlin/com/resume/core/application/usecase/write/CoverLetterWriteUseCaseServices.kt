package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.DeleteCoverLetterCommand
import com.resume.core.application.dto.write.SaveCoverLetterCommand
import com.resume.core.application.dto.write.UpdateCoverLetterCommand
import com.resume.core.domain.model.CoverLetter
import com.resume.core.port.outbound.persistence.CoverLetterRepositoryPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.UUID

@Service
class SaveCoverLetterUseCaseService(
    private val coverLetterRepository: CoverLetterRepositoryPort
) : SaveCoverLetterUseCase {

    override fun handle(command: SaveCoverLetterCommand): Mono<CoverLetter> {
        val now = LocalDateTime.now()
        val coverLetter = CoverLetter(
            id = UUID.randomUUID(),
            userId = command.userId,
            resumeId = command.resumeId,
            companyName = command.companyName,
            position = command.position,
            jobDescription = command.jobDescription,
            content = command.content,
            validationScore = command.validationScore,
            version = 1,
            createdAt = now,
            updatedAt = now
        )
        return coverLetterRepository.save(coverLetter)
    }
}

@Service
class UpdateCoverLetterUseCaseService(
    private val coverLetterRepository: CoverLetterRepositoryPort
) : UpdateCoverLetterUseCase {

    override fun handle(command: UpdateCoverLetterCommand): Mono<CoverLetter> {
        val now = LocalDateTime.now()
        return coverLetterRepository.findByIdAndUserId(command.id, command.userId)
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Cover letter not found"))
            )
            .flatMap { existing ->
                // 본문/메타 편집. validationScore 는 원 생성 점수를 보존(편집해도 이력 유지).
                val updated = existing.copy(
                    companyName = command.companyName,
                    position = command.position,
                    jobDescription = command.jobDescription,
                    content = command.content,
                    updatedAt = now,
                    version = existing.version + 1
                )
                coverLetterRepository.update(updated)
            }
    }
}

@Service
class DeleteCoverLetterUseCaseService(
    private val coverLetterRepository: CoverLetterRepositoryPort
) : DeleteCoverLetterUseCase {

    override fun handle(command: DeleteCoverLetterCommand): Mono<Unit> =
        coverLetterRepository.existsByIdAndUserId(command.id, command.userId)
            .flatMap { exists ->
                if (!exists) {
                    Mono.error<Unit>(
                        ResponseStatusException(HttpStatus.NOT_FOUND, "Cover letter not found")
                    )
                } else {
                    coverLetterRepository.deleteByIdAndUserId(command.id, command.userId)
                }
            }
}

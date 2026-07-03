package com.resume.core.application.usecase.write

import com.resume.core.application.dto.write.DeleteCoverLetterCommand
import com.resume.core.application.dto.write.SaveCoverLetterCommand
import com.resume.core.application.dto.write.UpdateCoverLetterCommand
import com.resume.core.domain.model.CoverLetter
import reactor.core.publisher.Mono

interface SaveCoverLetterUseCase {
    fun handle(command: SaveCoverLetterCommand): Mono<CoverLetter>
}

interface UpdateCoverLetterUseCase {
    fun handle(command: UpdateCoverLetterCommand): Mono<CoverLetter>
}

interface DeleteCoverLetterUseCase {
    fun handle(command: DeleteCoverLetterCommand): Mono<Unit>
}

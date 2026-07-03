package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GetCoverLetterQuery
import com.resume.core.application.dto.read.ListCoverLettersQuery
import com.resume.core.domain.model.CoverLetter
import com.resume.core.port.outbound.persistence.CoverLetterRepositoryPort
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class GetCoverLetterUseCaseService(
    private val coverLetterRepository: CoverLetterRepositoryPort
) : GetCoverLetterUseCase {

    override fun handle(query: GetCoverLetterQuery): Mono<CoverLetter> =
        coverLetterRepository.findByIdAndUserId(query.id, query.userId)
}

@Service
class ListCoverLettersUseCaseService(
    private val coverLetterRepository: CoverLetterRepositoryPort
) : ListCoverLettersUseCase {

    override fun handle(query: ListCoverLettersQuery): Flux<CoverLetter> =
        coverLetterRepository.findAllByUserId(query.userId)
}

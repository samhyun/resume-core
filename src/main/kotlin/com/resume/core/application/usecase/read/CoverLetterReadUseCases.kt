package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GetCoverLetterQuery
import com.resume.core.application.dto.read.ListCoverLettersQuery
import com.resume.core.domain.model.CoverLetter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface GetCoverLetterUseCase {
    fun handle(query: GetCoverLetterQuery): Mono<CoverLetter>
}

interface ListCoverLettersUseCase {
    fun handle(query: ListCoverLettersQuery): Flux<CoverLetter>
}

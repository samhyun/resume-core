package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GenerateResumePdfQuery
import com.resume.core.application.dto.read.ResumePdfResult
import reactor.core.publisher.Mono

fun interface GenerateResumePdfUseCase {
    fun handle(query: GenerateResumePdfQuery): Mono<ResumePdfResult>
}

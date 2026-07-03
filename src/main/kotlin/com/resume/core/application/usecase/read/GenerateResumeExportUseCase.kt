package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GenerateResumeExportQuery
import com.resume.core.application.dto.read.ResumeExportResult
import reactor.core.publisher.Mono

fun interface GenerateResumeExportUseCase {
    fun handle(query: GenerateResumeExportQuery): Mono<ResumeExportResult>
}

package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GenerateResumeExportQuery
import com.resume.core.application.dto.read.ResumeExportResult
import com.resume.core.domain.model.Resume
import com.resume.core.port.outbound.external.DocumentConversionPort
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import com.resume.core.port.outbound.rendering.ResumePlainTextRendererPort
import com.resume.core.port.outbound.rendering.ResumeTemplateRendererPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Service
class GenerateResumeExportUseCaseService(
    private val resumeRepository: ResumeRepositoryPort,
    private val resumeTemplateRendererPort: ResumeTemplateRendererPort,
    private val plainTextRendererPort: ResumePlainTextRendererPort,
    private val documentConversionPort: DocumentConversionPort
) : GenerateResumeExportUseCase {

    override fun handle(query: GenerateResumeExportQuery): Mono<ResumeExportResult> =
        resumeRepository.findByIdAndUserId(query.resumeId, query.userId)
            .switchIfEmpty(
                Mono.error(
                    ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                    )
                )
            )
            .flatMap { resume ->
                renderBytes(resume, query)
                    .map { bytes ->
                        ResumeExportResult(
                            fileName = buildFileName(resume, query),
                            contentType = query.format.contentType,
                            bytes = bytes
                        )
                    }
            }

    private fun renderBytes(resume: Resume, query: GenerateResumeExportQuery): Mono<ByteArray> =
        if (query.format.htmlBased) {
            resumeTemplateRendererPort.render(resume, query.templateType)
                .flatMap { html -> documentConversionPort.convert(html, query.format) }
        } else {
            // TXT: 외부 바이너리 없이 도메인 데이터에서 직접 생성 (결정적, IO 없음).
            Mono.fromCallable { plainTextRendererPort.render(resume).toByteArray(StandardCharsets.UTF_8) }
        }

    private fun buildFileName(resume: Resume, query: GenerateResumeExportQuery): String {
        val sanitizedHeadline = resume.resumeData.summary.headline
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { resume.id.toString() }

        // 템플릿은 HTML 기반 포맷에만 의미가 있으므로 TXT는 템플릿 접미사를 생략한다.
        return if (query.format.htmlBased) {
            "$sanitizedHeadline-${query.templateType.templateName}.${query.format.extension}"
        } else {
            "$sanitizedHeadline.${query.format.extension}"
        }
    }
}

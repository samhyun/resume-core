package com.resume.core.application.usecase.read

import com.resume.core.application.dto.read.GenerateResumePdfQuery
import com.resume.core.application.dto.read.ResumePdfResult
import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeTemplateType
import com.resume.core.port.outbound.external.PdfConversionPort
import com.resume.core.port.outbound.persistence.ResumeRepositoryPort
import com.resume.core.port.outbound.rendering.ResumeTemplateRendererPort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

@Service
class GenerateResumePdfUseCaseService(
    private val resumeRepository: ResumeRepositoryPort,
    private val resumeTemplateRendererPort: ResumeTemplateRendererPort,
    private val pdfConversionPort: PdfConversionPort
) : GenerateResumePdfUseCase {

    override fun handle(query: GenerateResumePdfQuery): Mono<ResumePdfResult> =
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
                resumeTemplateRendererPort
                    .render(resume, query.templateType)
                    .flatMap { html ->
                        pdfConversionPort.convert(html)
                            .map { bytes ->
                                ResumePdfResult(
                                    fileName = buildFileName(resume, query.templateType),
                                    bytes = bytes
                                )
                            }
                    }
            }

    private fun buildFileName(resume: Resume, templateType: ResumeTemplateType): String {
        val sanitizedHeadline = resume.resumeData.summary.headline
            .lowercase()
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { resume.id.toString() }

        return "${sanitizedHeadline}-${templateType.templateName}.pdf"
    }
}

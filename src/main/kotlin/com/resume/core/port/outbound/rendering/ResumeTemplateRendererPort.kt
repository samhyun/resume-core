package com.resume.core.port.outbound.rendering

import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeTemplateType
import reactor.core.publisher.Mono

/**
 * Port for rendering resume templates into HTML using the selected theme.
 */
fun interface ResumeTemplateRendererPort {
    fun render(resume: Resume, templateType: ResumeTemplateType): Mono<String>
}

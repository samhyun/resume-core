package com.resume.core.adapter.outbound.rendering

import com.resume.core.domain.model.Resume
import com.resume.core.domain.model.ResumeTemplateType
import com.resume.core.port.outbound.rendering.ResumeTemplateRendererPort
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.OffsetDateTime
import java.util.Locale

@Component
class ThymeleafResumeTemplateRenderer(
    private val templateEngine: SpringTemplateEngine
) : ResumeTemplateRendererPort {

    override fun render(resume: Resume, templateType: ResumeTemplateType): Mono<String> =
        Mono.fromCallable {
            val context = Context(Locale.getDefault()).apply {
                setVariable("resume", resume)
                setVariable("templateName", templateType.templateName)
                setVariable("generatedAt", OffsetDateTime.now())
            }
            templateEngine.process(templateType.templatePath, context)
        }.subscribeOn(Schedulers.boundedElastic())
}

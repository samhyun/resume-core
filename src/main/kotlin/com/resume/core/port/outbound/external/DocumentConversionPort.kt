package com.resume.core.port.outbound.external

import com.resume.core.domain.model.ResumeExportFormat
import reactor.core.publisher.Mono

/**
 * Port for converting rendered HTML into a binary document (PDF, PNG, ...).
 *
 * Only [ResumeExportFormat.htmlBased] formats are supported here; text formats are produced
 * upstream from the domain model and never reach this port.
 */
fun interface DocumentConversionPort {
    fun convert(html: String, format: ResumeExportFormat): Mono<ByteArray>
}

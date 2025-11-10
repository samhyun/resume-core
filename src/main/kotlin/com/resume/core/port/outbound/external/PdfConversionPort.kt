package com.resume.core.port.outbound.external

import reactor.core.publisher.Mono

/**
 * Port for converting rendered HTML into a PDF binary.
 */
fun interface PdfConversionPort {
    fun convert(html: String): Mono<ByteArray>
}

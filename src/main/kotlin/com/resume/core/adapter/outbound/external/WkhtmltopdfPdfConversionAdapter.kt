package com.resume.core.adapter.outbound.external

import com.resume.core.config.ResumePdfProperties
import com.resume.core.port.outbound.external.PdfConversionPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Component
class WkhtmltopdfPdfConversionAdapter(
    private val resumePdfProperties: ResumePdfProperties
) : PdfConversionPort {

    override fun convert(html: String): Mono<ByteArray> =
        Mono.fromCallable {
            val htmlFile = Files.createTempFile("resume-html", ".html")
            val pdfFile = Files.createTempFile("resume-pdf", ".pdf")
            try {
                Files.writeString(htmlFile, html, StandardCharsets.UTF_8)

                val process = runConversion(htmlFile, pdfFile)

                if (!process.waitFor(resumePdfProperties.timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    throw IllegalStateException(
                        "wkhtmltopdf timed out after ${resumePdfProperties.timeoutSeconds} seconds"
                    )
                }

                val exitCode = process.exitValue()
                if (exitCode != 0) {
                    val errors = process.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                    throw IllegalStateException("wkhtmltopdf failed with exit code $exitCode: $errors")
                }

                Files.readAllBytes(pdfFile)
            } catch (ex: IOException) {
                throw IllegalStateException(
                    "Failed to execute wkhtmltopdf. Install it locally or update resume.pdf.wkhtmltopdf-path",
                    ex
                )
            } finally {
                Files.deleteIfExists(htmlFile)
                Files.deleteIfExists(pdfFile)
            }
        }.subscribeOn(Schedulers.boundedElastic())

    private fun runConversion(htmlFile: Path, pdfFile: Path): Process {
        val command = listOf(
            resumePdfProperties.wkhtmltopdfPath,
            "--quiet",
            "--encoding", "utf-8",
            "--margin-top", "10mm",
            "--margin-bottom", "10mm",
            "--margin-left", "8mm",
            "--margin-right", "8mm",
            htmlFile.toAbsolutePath().toString(),
            pdfFile.toAbsolutePath().toString()
        )

        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }
}

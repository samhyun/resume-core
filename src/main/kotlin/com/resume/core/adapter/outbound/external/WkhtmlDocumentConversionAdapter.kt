package com.resume.core.adapter.outbound.external

import com.resume.core.config.ResumePdfProperties
import com.resume.core.domain.model.ResumeExportFormat
import com.resume.core.port.outbound.external.DocumentConversionPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Converts rendered HTML into PDF (wkhtmltopdf) or PNG (wkhtmltoimage).
 *
 * Both binaries share the same temp-file / process / timeout machinery; only the executable
 * path and CLI options differ per [ResumeExportFormat].
 */
@Component
class WkhtmlDocumentConversionAdapter(
    private val resumePdfProperties: ResumePdfProperties
) : DocumentConversionPort {

    override fun convert(html: String, format: ResumeExportFormat): Mono<ByteArray> =
        Mono.fromCallable {
            require(format.htmlBased) { "Only HTML-based formats are supported, got $format" }

            val htmlFile = Files.createTempFile("resume-html", ".html")
            val outputFile = Files.createTempFile("resume-out", ".${format.extension}")
            try {
                Files.writeString(htmlFile, html, StandardCharsets.UTF_8)

                val process = runConversion(htmlFile, outputFile, format)

                if (!process.waitFor(resumePdfProperties.timeoutSeconds, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                    throw IllegalStateException(
                        "${binaryFor(format)} timed out after ${resumePdfProperties.timeoutSeconds} seconds"
                    )
                }

                val exitCode = process.exitValue()
                if (exitCode != 0) {
                    val errors = process.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                    throw IllegalStateException("${binaryFor(format)} failed with exit code $exitCode: $errors")
                }

                Files.readAllBytes(outputFile)
            } catch (ex: IOException) {
                throw IllegalStateException(
                    "Failed to execute ${binaryFor(format)}. Install it locally or update resume.pdf.* paths",
                    ex
                )
            } finally {
                Files.deleteIfExists(htmlFile)
                Files.deleteIfExists(outputFile)
            }
        }.subscribeOn(Schedulers.boundedElastic())

    private fun binaryFor(format: ResumeExportFormat): String = when (format) {
        ResumeExportFormat.PDF -> resumePdfProperties.wkhtmltopdfPath
        ResumeExportFormat.PNG -> resumePdfProperties.wkhtmltoimagePath
        ResumeExportFormat.TXT -> throw IllegalArgumentException("TXT is not an HTML-based format")
    }

    private fun runConversion(htmlFile: Path, outputFile: Path, format: ResumeExportFormat): Process {
        val command = buildList {
            add(binaryFor(format))
            when (format) {
                ResumeExportFormat.PDF -> addAll(
                    listOf(
                        "--quiet",
                        "--encoding", "utf-8",
                        "--margin-top", "10mm",
                        "--margin-bottom", "10mm",
                        "--margin-left", "8mm",
                        "--margin-right", "8mm"
                    )
                )
                ResumeExportFormat.PNG -> addAll(
                    listOf(
                        "--quiet",
                        "--encoding", "utf-8",
                        "--format", "png",
                        "--width", resumePdfProperties.pngWidth.toString()
                    )
                )
                ResumeExportFormat.TXT -> throw IllegalArgumentException("TXT is not an HTML-based format")
            }
            add(htmlFile.toAbsolutePath().toString())
            add(outputFile.toAbsolutePath().toString())
        }

        return ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
    }
}

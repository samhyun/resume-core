package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.model.GetResumeResponse
import com.resume.core.adapter.inbound.web.model.SaveResumeRequest
import com.resume.core.adapter.inbound.web.model.SaveResumeResponse
import com.resume.core.adapter.inbound.web.model.UpdateResumeResponse
import com.resume.core.application.dto.read.GetActiveResumeQuery
import com.resume.core.application.dto.read.GetResumeQuery
import com.resume.core.application.dto.read.ListResumesQuery
import com.resume.core.application.dto.read.GenerateResumeExportQuery
import com.resume.core.application.usecase.read.GetActiveResumeUseCase
import com.resume.core.application.usecase.read.GetResumeUseCase
import com.resume.core.application.usecase.read.ListResumesUseCase
import com.resume.core.application.usecase.read.GenerateResumeExportUseCase
import com.resume.core.application.usecase.write.SaveResumeUseCase
import com.resume.core.application.usecase.write.UpdateResumeUseCase
import com.resume.core.application.usecase.write.DeleteResumeUseCase
import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.adapter.inbound.web.support.toUuidOrBadRequest
import com.resume.core.domain.model.ResumeExportFormat
import com.resume.core.domain.model.ResumeTemplateType
import com.resume.core.application.dto.write.DeleteResumeCommand
import org.springframework.http.HttpStatus
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * REST controller for resume operations
 * Handles resume creation, retrieval, and management
 */
@RestController
@RequestMapping("/api/resume-core/resumes")
class ResumeController(
    private val saveResumeUseCase: SaveResumeUseCase,
    private val updateResumeUseCase: UpdateResumeUseCase,
    private val getResumeUseCase: GetResumeUseCase,
    private val getActiveResumeUseCase: GetActiveResumeUseCase,
    private val listResumesUseCase: ListResumesUseCase,
    private val deleteResumeUseCase: DeleteResumeUseCase,
    private val generateResumeExportUseCase: GenerateResumeExportUseCase,
    private val authenticationFacade: ReactiveJwtAuthenticationFacade
) {

    /**
     * Save a new resume
     * Automatically deactivates any existing active resume for the user
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun saveResume(
        @RequestBody request: SaveResumeRequest
    ): Mono<SaveResumeResponse> {
        return authenticationFacade.currentUserId()
            .flatMap { userId ->
                saveResumeUseCase
                    .handle(request.toCommand(userId))
                    .map(SaveResumeResponse::from)
            }
    }

    /**
     * List all resumes owned by the authenticated user ordered by creation date desc
     */
    @GetMapping
    fun listResumes(): Mono<List<GetResumeResponse>> {
        return authenticationFacade.currentUserId()
            .flatMapMany { userId ->
                listResumesUseCase
                    .handle(ListResumesQuery(userId))
            }
            .map(GetResumeResponse::from)
            .collectList()
    }

    /**
     * Update an existing resume by ID
     * Requires ownership validation through authentication facade
     */
    @PutMapping("/{resumeId}")
    fun updateResume(
        @PathVariable resumeId: String,
        @RequestBody request: SaveResumeRequest
    ): Mono<UpdateResumeResponse> {
        val uuid = resumeId.toUuidOrBadRequest("resumeId")

        return authenticationFacade.currentUserId()
            .flatMap { userId ->
                updateResumeUseCase
                    .handle(request.toUpdateCommand(uuid, userId))
                    .map(UpdateResumeResponse::from)
            }
    }

    /**
     * Get a specific resume by ID
     * Users can only access their own resumes
     */
    @GetMapping("/{resumeId}")
    fun getResume(
        @PathVariable resumeId: String
    ): Mono<GetResumeResponse> {
        val uuid = resumeId.toUuidOrBadRequest("resumeId")

        return authenticationFacade.currentUserId()
            .flatMap { userId ->
                getResumeUseCase
                    .handle(GetResumeQuery(uuid, userId))
                    .map(GetResumeResponse::from)
                    .switchIfEmpty(
                        Mono.error(
                            ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Resume not found"
                            )
                        )
                    )
            }
    }

    /**
     * Get the user's currently active resume
     */
    @GetMapping("/active")
    fun getActiveResume(): Mono<GetResumeResponse> {
        return authenticationFacade.currentUserId()
            .flatMap { userId ->
                getActiveResumeUseCase
                    .handle(GetActiveResumeQuery(userId))
                    .map(GetResumeResponse::from)
                    .switchIfEmpty(
                        Mono.error(
                            ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No active resume found"
                            )
                        )
                    )
            }
    }

    /**
     * Download the resume as a PDF generated from the selected template.
     *
     * Kept for backward compatibility; equivalent to `/export?format=pdf`.
     */
    @GetMapping("/{resumeId}/pdf")
    fun downloadResumePdf(
        @PathVariable resumeId: String,
        @RequestParam(name = "template", defaultValue = "default") template: String
    ): Mono<ResponseEntity<ByteArray>> =
        export(resumeId, template, ResumeExportFormat.PDF)

    /**
     * Export the resume in the requested format (pdf | png | txt) from the selected template.
     * `template` is ignored for txt.
     */
    @GetMapping("/{resumeId}/export")
    fun exportResume(
        @PathVariable resumeId: String,
        @RequestParam(name = "format", defaultValue = "pdf") format: String,
        @RequestParam(name = "template", defaultValue = "default") template: String
    ): Mono<ResponseEntity<ByteArray>> {
        val exportFormat = ResumeExportFormat.from(format)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported export format: $format"
            )
        return export(resumeId, template, exportFormat)
    }

    private fun export(
        resumeId: String,
        template: String,
        format: ResumeExportFormat
    ): Mono<ResponseEntity<ByteArray>> {
        val uuid = resumeId.toUuidOrBadRequest("resumeId")
        val templateType = ResumeTemplateType.fromValue(template)

        return authenticationFacade.currentUserId()
            .flatMap { userId ->
                generateResumeExportUseCase.handle(
                    GenerateResumeExportQuery(
                        resumeId = uuid,
                        userId = userId,
                        templateType = templateType,
                        format = format
                    )
                )
            }
            .map { result ->
                ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"${result.fileName}\""
                    )
                    .contentType(MediaType.parseMediaType(result.contentType))
                    .body(result.bytes)
            }
    }

    /**
     * Delete a resume owned by the authenticated user
     */
    @DeleteMapping("/{resumeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteResume(
        @PathVariable resumeId: String
    ): Mono<Unit> {
        val uuid = resumeId.toUuidOrBadRequest("resumeId")

        return authenticationFacade.currentUserId()
            .flatMap { userId ->
                deleteResumeUseCase.handle(DeleteResumeCommand(uuid, userId))
            }
    }

}

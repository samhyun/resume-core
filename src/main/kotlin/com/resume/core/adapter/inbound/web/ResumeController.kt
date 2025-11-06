package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.model.GetResumeResponse
import com.resume.core.adapter.inbound.web.model.SaveResumeRequest
import com.resume.core.adapter.inbound.web.model.SaveResumeResponse
import com.resume.core.adapter.inbound.web.model.UpdateResumeResponse
import com.resume.core.application.dto.read.GetActiveResumeQuery
import com.resume.core.application.dto.read.GetResumeQuery
import com.resume.core.application.dto.read.ListResumesQuery
import com.resume.core.application.usecase.read.GetActiveResumeUseCase
import com.resume.core.application.usecase.read.GetResumeUseCase
import com.resume.core.application.usecase.read.ListResumesUseCase
import com.resume.core.application.usecase.write.SaveResumeUseCase
import com.resume.core.application.usecase.write.UpdateResumeUseCase
import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.util.UUID

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
        val uuid = resumeId.toUuidOrBadRequest()

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
        val uuid = resumeId.toUuidOrBadRequest()

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
     * Extract user ID from JWT token
     * Uses 'sub' claim as the user identifier
     */
    /**
     * Convert string to UUID with validation
     */
    private fun String.toUuidOrBadRequest(): UUID {
        val trimmed = trim()
        if (trimmed.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Resume ID must not be blank"
            )
        }
        return try {
            UUID.fromString(trimmed)
        } catch (ex: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid resume ID format"
            )
        }
    }
}

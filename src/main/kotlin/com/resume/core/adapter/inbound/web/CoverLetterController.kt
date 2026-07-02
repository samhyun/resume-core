package com.resume.core.adapter.inbound.web

import com.resume.core.adapter.inbound.web.model.CoverLetterResponse
import com.resume.core.adapter.inbound.web.model.GenerateCoverLetterRequest
import com.resume.core.adapter.inbound.web.model.SaveCoverLetterRequest
import com.resume.core.adapter.inbound.web.support.ReactiveJwtAuthenticationFacade
import com.resume.core.adapter.inbound.web.support.toUuidOrBadRequest
import com.resume.core.application.dto.read.GetCoverLetterQuery
import com.resume.core.application.dto.read.ListCoverLettersQuery
import com.resume.core.application.dto.write.DeleteCoverLetterCommand
import com.resume.core.application.usecase.read.GetCoverLetterUseCase
import com.resume.core.application.usecase.read.ListCoverLettersUseCase
import com.resume.core.application.usecase.write.DeleteCoverLetterUseCase
import com.resume.core.application.usecase.write.GenerateCoverLetterUseCase
import com.resume.core.application.usecase.write.SaveCoverLetterUseCase
import com.resume.core.application.usecase.write.UpdateCoverLetterUseCase
import com.resume.core.port.outbound.external.AiAgentStreamEvent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * REST controller for cover letter CRUD. All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/resume-core/cover-letters")
class CoverLetterController(
    private val saveCoverLetterUseCase: SaveCoverLetterUseCase,
    private val updateCoverLetterUseCase: UpdateCoverLetterUseCase,
    private val getCoverLetterUseCase: GetCoverLetterUseCase,
    private val listCoverLettersUseCase: ListCoverLettersUseCase,
    private val deleteCoverLetterUseCase: DeleteCoverLetterUseCase,
    private val generateCoverLetterUseCase: GenerateCoverLetterUseCase,
    private val authenticationFacade: ReactiveJwtAuthenticationFacade
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: SaveCoverLetterRequest): Mono<CoverLetterResponse> =
        authenticationFacade.currentUserId()
            .flatMap { userId -> saveCoverLetterUseCase.handle(request.toCommand(userId)) }
            .map(CoverLetterResponse::from)

    /**
     * Generate a cover letter via the cover_letter agent (core-orchestrated, **streamed** as SSE).
     * Core injects resume_data + auto-answers the company-info prompt, then relays the pipeline
     * stream. The client reads the final text from the stream and persists it via `POST /cover-letters`.
     */
    @PostMapping("/generate", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun generate(@RequestBody request: GenerateCoverLetterRequest): Flux<ServerSentEvent<String>> =
        authenticationFacade.currentUserId()
            .flatMapMany { userId -> generateCoverLetterUseCase.stream(request.toCommand(userId)) }
            .map { it.toServerSentEvent() }

    @GetMapping
    fun list(): Flux<CoverLetterResponse> =
        authenticationFacade.currentUserId()
            .flatMapMany { userId -> listCoverLettersUseCase.handle(ListCoverLettersQuery(userId)) }
            .map(CoverLetterResponse::from)

    @GetMapping("/{id}")
    fun get(@PathVariable id: String): Mono<CoverLetterResponse> {
        val uuid = id.toUuidOrBadRequest()
        return authenticationFacade.currentUserId()
            .flatMap { userId -> getCoverLetterUseCase.handle(GetCoverLetterQuery(uuid, userId)) }
            .switchIfEmpty(
                Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "Cover letter not found"))
            )
            .map(CoverLetterResponse::from)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @RequestBody request: SaveCoverLetterRequest
    ): Mono<CoverLetterResponse> {
        val uuid = id.toUuidOrBadRequest()
        return authenticationFacade.currentUserId()
            .flatMap { userId -> updateCoverLetterUseCase.handle(request.toUpdateCommand(uuid, userId)) }
            .map(CoverLetterResponse::from)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: String): Mono<Unit> {
        val uuid = id.toUuidOrBadRequest()
        return authenticationFacade.currentUserId()
            .flatMap { userId -> deleteCoverLetterUseCase.handle(DeleteCoverLetterCommand(uuid, userId)) }
    }

    private fun AiAgentStreamEvent.toServerSentEvent(): ServerSentEvent<String> {
        val builder = ServerSentEvent.builder<String>()
        id?.let(builder::id)
        event?.let(builder::event)
        retry?.let { builder.retry(Duration.ofMillis(it)) }
        comment?.let(builder::comment)
        data?.let(builder::data)
        return builder.build()
    }
}

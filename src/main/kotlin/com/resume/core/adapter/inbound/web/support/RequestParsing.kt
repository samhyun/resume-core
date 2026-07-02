package com.resume.core.adapter.inbound.web.support

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * 경로 변수/폼 필드 문자열을 UUID로 파싱한다.
 * 비어 있거나 형식이 잘못되면 400(Bad Request)으로 응답한다.
 *
 * @param fieldName 오류 메시지에 표기할 필드 이름 (예: "resumeId", "sessionId")
 */
fun String.toUuidOrBadRequest(fieldName: String = "id"): UUID {
    val trimmed = trim()
    if (trimmed.isEmpty()) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$fieldName must not be blank")
    }
    return try {
        UUID.fromString(trimmed)
    } catch (ex: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid $fieldName format")
    }
}

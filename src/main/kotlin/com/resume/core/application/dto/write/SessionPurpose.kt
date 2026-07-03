package com.resume.core.application.dto.write

import com.fasterxml.jackson.annotation.JsonValue

/**
 * AI 에이전트 채팅 세션의 목적. 에이전트 세션 state 에 주입된다.
 */
enum class SessionPurpose(@get:JsonValue val value: String) {
    RESUME_UPGRADE("resume_upgrade"),
    INTERVIEW_PREP("interview_prep"),
    GENERAL("general"),
}

package com.resume.core.application.dto.write

data class CreateSessionCommand(
    val ids: SessionIds,
    val purpose: SessionPurpose = SessionPurpose.GENERAL,
    // cover_letter / interview 에이전트가 세션 state에서 읽는 이력서 JSON 문자열 (resume_data). null이면 미주입.
    val resumeData: String? = null
)

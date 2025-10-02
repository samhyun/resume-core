package com.resume.core.application.dto.write

data class CreateSessionCommand(
    val ids: SessionIds,
    val purpose: SessionPurpose = SessionPurpose.GENERAL
)

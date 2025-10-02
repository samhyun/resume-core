package com.resume.core.application.dto.write

import org.springframework.http.codec.multipart.FilePart
import java.util.UUID

data class RunChatSessionCommand(
    val sessionId: UUID,
    val message: ChatMessagePayload
) {
    companion object {
        fun text(sessionId: UUID, text: String): RunChatSessionCommand =
            RunChatSessionCommand(sessionId, ChatMessagePayload.Text(text))

        fun file(
            sessionId: UUID,
            displayName: String?,
            mimeType: String,
            part: FilePart
        ): RunChatSessionCommand =
            RunChatSessionCommand(
                sessionId = sessionId,
                message = ChatMessagePayload.File(
                    displayName = displayName,
                    mimeType = mimeType,
                    part = part
                )
            )
    }
}

sealed interface ChatMessagePayload {
    data class Text(val text: String) : ChatMessagePayload
    data class File(
        val displayName: String?,
        val mimeType: String,
        val part: FilePart
    ) : ChatMessagePayload
}

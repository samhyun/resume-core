package com.resume.core.application.dto.write

import org.springframework.http.codec.multipart.FilePart
import java.util.UUID

data class RunChatSessionCommand(
    val sessionId: UUID,
    // 인증된 사용자(JWT sub). 유스케이스에서 세션 소유권 검증에 사용한다.
    val userId: String,
    val message: ChatMessagePayload,
) {
    companion object {
        fun text(sessionId: UUID, userId: String, text: String): RunChatSessionCommand =
            RunChatSessionCommand(sessionId, userId, ChatMessagePayload.Text(text))

        fun file(
            sessionId: UUID,
            userId: String,
            displayName: String?,
            mimeType: String,
            part: FilePart,
        ): RunChatSessionCommand =
            RunChatSessionCommand(
                sessionId = sessionId,
                userId = userId,
                message = ChatMessagePayload.File(
                    displayName = displayName,
                    mimeType = mimeType,
                    part = part,
                ),
            )

        fun functionResponse(
            sessionId: UUID,
            userId: String,
            id: String,
            name: String,
            result: String,
        ): RunChatSessionCommand =
            RunChatSessionCommand(
                sessionId = sessionId,
                userId = userId,
                message = ChatMessagePayload.FunctionResponse(
                    id = id,
                    name = name,
                    result = result,
                ),
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

    /** ADK 2.0 HITL 인터럽트 답변(function_response 재개). id는 받은 interrupt id를 echo. */
    data class FunctionResponse(
        val id: String,
        val name: String,
        val result: String
    ) : ChatMessagePayload
}

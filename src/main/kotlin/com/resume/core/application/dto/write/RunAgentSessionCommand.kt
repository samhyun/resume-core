package com.resume.core.application.dto.write

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class RunAgentSessionCommand(
    val appName: String,
    val userId: String,
    val sessionId: String,
    val newMessage: AgentMessage
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentMessage(
    val role: String,
    val parts: List<AgentMessagePart>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentMessagePart(
    val text: String? = null,
    val inlineData: AgentInlineData? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentInlineData(
    val displayName: String? = null,
    val data: String,
    val mimeType: String
)

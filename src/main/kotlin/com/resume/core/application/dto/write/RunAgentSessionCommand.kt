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
    val inlineData: AgentInlineData? = null,
    val functionResponse: AgentFunctionResponse? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentInlineData(
    val displayName: String? = null,
    val data: String,
    val mimeType: String
)

/**
 * ADK 2.0 그래프의 RequestInput 인터럽트(HITL)를 재개할 때 보내는 function_response 파트.
 * 클라이언트가 보관한 interrupt id를 그대로 echo 하고, name 은 항상 "adk_request_input",
 * response 는 사용자 답변을 담은 자유 형식 dict(보통 {"result": "<답변>"}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentFunctionResponse(
    val id: String,
    val name: String,
    val response: Map<String, Any?>
)

package com.resume.core.adapter.outbound.client

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode
import com.resume.core.domain.model.UserProfile
import com.resume.core.port.outbound.external.UserProfilePort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono

/**
 * Proxies the user's own profile read/edit to the Keycloak Account REST API
 * (`{realm-url}/account`), forwarding the caller's access token. Editing `firstName`/`lastName`
 * only — other fields (username/email/emailVerified) are read-only here.
 *
 * NOTE: Keycloak must accept the caller's token at the account endpoint (the app client's tokens
 * need the `account` audience / client scope); otherwise Keycloak returns 401/403 which is
 * propagated to the caller as-is (see [toResponseError]).
 */
@Component
class KeycloakAccountClient(
    builder: WebClient.Builder,
    @param:Value("\${keycloak.realm-url:http://localhost:8080/realms/aura}") realmUrl: String
) : UserProfilePort {

    private val client = builder.baseUrl(realmUrl).build()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getProfile(accessToken: String): Mono<UserProfile> =
        getAccountNode(accessToken).map(::toUserProfile)

    override fun updateProfile(accessToken: String, firstName: String?, lastName: String?): Mono<UserProfile> =
        getAccountNode(accessToken)
            .flatMap { current ->
                if (current !is ObjectNode) {
                    Mono.error(ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unexpected Keycloak account response"))
                } else {
                    current.remove("userProfileMetadata") // read-only 메타는 되돌려보내지 않음
                    setOrNull(current, "firstName", firstName)
                    setOrNull(current, "lastName", lastName)
                    client.post()
                        .uri(ACCOUNT_PATH)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(current)
                        .retrieve()
                        .onStatus({ it.isError }, ::toResponseError)
                        .toBodilessEntity()
                        .then()
                }
            }
            // 저장 후 Keycloak 상태를 다시 읽어 반환(로컬 요청 바디가 아니라 정규화된 실제 값).
            .then(getProfile(accessToken))

    private fun getAccountNode(accessToken: String): Mono<JsonNode> =
        client.get()
            .uri(ACCOUNT_PATH)
            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .onStatus({ it.isError }, ::toResponseError)
            .bodyToMono(JsonNode::class.java)

    /**
     * Keycloak 의 4xx/5xx 를 같은 status 의 ResponseStatusException 으로 전파(500으로 뭉개지지 않게).
     * `createException()` 이 에러 body 를 drain/release 하므로 커넥션 누수를 막는다. 업스트림 상세는
     * 서버 로그로만 남기고(HTML/5xx 상세 유출 방지) 클라이언트엔 일반 메시지만 준다.
     */
    private fun toResponseError(response: ClientResponse): Mono<out Throwable> =
        response.createException().map { ex ->
            log.warn("Keycloak account request failed: {}", response.statusCode())
            log.debug("Keycloak account error body: {}", ex.responseBodyAsString.take(500)) // PII 가능성 → debug 레벨
            ResponseStatusException(response.statusCode(), "Keycloak account request failed")
        }

    private fun setOrNull(node: ObjectNode, field: String, value: String?) {
        if (value != null) node.put(field, value) else node.putNull(field)
    }

    private fun toUserProfile(node: JsonNode): UserProfile {
        val username = node.path("username").asText().takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak account response missing username")
        return UserProfile(
            username = username,
            email = node.path("email").asText().takeIf { it.isNotBlank() },
            firstName = node.path("firstName").asText().takeIf { it.isNotBlank() },
            lastName = node.path("lastName").asText().takeIf { it.isNotBlank() },
            emailVerified = node.path("emailVerified").asBoolean(false)
        )
    }

    private fun bearer(accessToken: String): String = "Bearer $accessToken"

    private companion object {
        const val ACCOUNT_PATH = "/account"
    }
}

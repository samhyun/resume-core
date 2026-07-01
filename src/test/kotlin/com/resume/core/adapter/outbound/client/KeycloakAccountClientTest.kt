package com.resume.core.adapter.outbound.client

import tools.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import reactor.test.StepVerifier

class KeycloakAccountClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: KeycloakAccountClient

    @BeforeEach
    fun setup() {
        server = MockWebServer().also { it.start() }
        val realmUrl = server.url("/").toString().removeSuffix("/")
        client = KeycloakAccountClient(WebClient.builder(), realmUrl)
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getProfile forwards the bearer token and maps the account`() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"username":"tester","email":"t@e.com","firstName":"길동","lastName":"홍","emailVerified":true}"""
                )
        )

        StepVerifier.create(client.getProfile("tok"))
            .assertNext { profile ->
                assertThat(profile.username).isEqualTo("tester")
                assertThat(profile.email).isEqualTo("t@e.com")
                assertThat(profile.firstName).isEqualTo("길동")
                assertThat(profile.emailVerified).isTrue()
            }
            .verifyComplete()

        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("GET")
        assertThat(recorded.path).isEqualTo("/account")
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer tok")
    }

    @Test
    fun `updateProfile reads the account then posts modified names`() {
        // 1) 초기 GET  2) POST(204)  3) 갱신 후 재조회 GET
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"username":"tester","email":"t@e.com","firstName":"old","lastName":"old","emailVerified":true,"userProfileMetadata":{"x":1}}"""
                )
        )
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """{"username":"tester","email":"t@e.com","firstName":"새길동","lastName":"새홍","emailVerified":true}"""
                )
        )

        StepVerifier.create(client.updateProfile("tok", "새길동", "새홍"))
            .assertNext { profile ->
                assertThat(profile.firstName).isEqualTo("새길동")
                assertThat(profile.lastName).isEqualTo("새홍")
                assertThat(profile.username).isEqualTo("tester")
            }
            .verifyComplete()

        val get1 = server.takeRequest()
        assertThat(get1.method).isEqualTo("GET")

        val post = server.takeRequest()
        assertThat(post.method).isEqualTo("POST")
        assertThat(post.getHeader("Authorization")).isEqualTo("Bearer tok")
        assertThat(post.getHeader("Content-Type")).contains("application/json")
        val body = jacksonObjectMapper().readTree(post.body.readUtf8())
        assertThat(body.path("firstName").asText()).isEqualTo("새길동")
        assertThat(body.path("lastName").asText()).isEqualTo("새홍")
        // read-only 메타는 POST 에서 제거됨
        assertThat(body.has("userProfileMetadata")).isFalse()

        val get2 = server.takeRequest()
        assertThat(get2.method).isEqualTo("GET") // 저장 후 재조회
    }

    @Test
    fun `propagates the Keycloak error status instead of 500`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error":"forbidden"}""")
        )

        StepVerifier.create(client.getProfile("tok"))
            .expectErrorSatisfies { error ->
                assertThat(error).isInstanceOf(ResponseStatusException::class.java)
                assertThat((error as ResponseStatusException).statusCode.value()).isEqualTo(403)
            }
            .verify()
    }
}

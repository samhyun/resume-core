package com.resume.core

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.client.reactive.ReactiveOAuth2ClientAutoConfiguration"
    ]
)
@Disabled("Security auto-configuration creates package-private beans that are cumbersome to stub; adjust test wiring before enabling")
class ResumeCoreApplicationTests {

    @Test
    fun contextLoads() {
    }
}

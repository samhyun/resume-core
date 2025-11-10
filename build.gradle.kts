plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    id("com.epages.restdocs-api-spec") version "0.18.2"
//    kotlin("plugin.jpa") version "1.9.25"
    kotlin("kapt") version "2.2.10"
}

group = "com.resume"
version = "0.0.1-SNAPSHOT"
description = "resume-core"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")

val snippetsDir = project.extra["snippetsDir"] as File


val restdocsApiSpecVersion = "0.18.2"
val mockkVersion = "1.13.12"
val mockitoKotlinVersion = "5.4.0"
val postgresDriverVersion = "42.7.7"
val r2dbcPostgresVersion = "1.0.7.RELEASE"
val flywayVersion = "11.12.0"
val tikaVersion = "2.9.2"
val kotlinxCoroutinesVersion = "1.8.1"
val mockwebserverVersion = "4.12.0"


//apply(from = "${rootDir}/gradle/querydsl.gradle")


dependencies {
//    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
//    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
    implementation("org.postgresql:r2dbc-postgresql:$r2dbcPostgresVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.apache.tika:tika-core:$tikaVersion")
    implementation("org.apache.tika:tika-parsers-standard-package:$tikaVersion")

    // https://mvnrepository.com/artifact/org.postgresql/postgresql
    runtimeOnly("org.postgresql:postgresql:$postgresDriverVersion")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-webtestclient")
    testImplementation("com.epages:restdocs-api-spec:$restdocsApiSpecVersion")
    testImplementation("com.epages:restdocs-api-spec-webtestclient:$restdocsApiSpecVersion")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
    testImplementation("com.squareup.okhttp3:mockwebserver:$mockwebserverVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

openapi3 {
    setServer("http://localhost:8080")
    title = "Resume Core API"
    description = "API documentation for resume-core"
    version = project.version.toString()
    snippetsDirectory = snippetsDir.path
    outputDirectory = "build/api-spec"
    outputFileNamePrefix = "resume-core"
    format = "yaml"
}

afterEvaluate {
    tasks.named("openapi3") {
        dependsOn(tasks.test)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    outputs.dir(snippetsDir)
}

tasks.asciidoctor {
    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
}

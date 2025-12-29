package com.resume.core

import com.resume.core.config.ResumePdfProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ResumePdfProperties::class)
class ResumeCoreApplication

fun main(args: Array<String>) {
    runApplication<ResumeCoreApplication>(*args)
}

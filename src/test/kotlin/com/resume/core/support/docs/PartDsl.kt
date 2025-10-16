package com.resume.core.support.docs

import org.springframework.restdocs.request.RequestPartDescriptor
import org.springframework.restdocs.request.RequestDocumentation.partWithName

class PartDsl internal constructor() {
    private val descriptors = mutableListOf<RequestPartDescriptor>()

    infix fun String.means(description: String): PartSpec = PartSpec(this, description)

    inner class PartSpec internal constructor(
        private val name: String,
        private val description: String,
        private var isOptional: Boolean = false
    ) {
        infix fun optional(flag: Boolean) {
            isOptional = flag
            val descriptor = partWithName(name).description(description)
            descriptors += if (isOptional) descriptor.optional() else descriptor
        }
    }

    fun build(): List<RequestPartDescriptor> = descriptors
}

fun parts(block: PartDsl.() -> Unit): Array<RequestPartDescriptor> {
    val dsl = PartDsl()
    dsl.block()
    return dsl.build().toTypedArray()
}
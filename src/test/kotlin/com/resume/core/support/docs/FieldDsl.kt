package com.resume.core.support.docs

import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath

class FieldDsl internal constructor() {
    private val descriptors = mutableListOf<FieldDescriptor>()

    infix fun String.type(type: DocsFieldType): FieldSpec = FieldSpec(this, type)

    inner class FieldSpec internal constructor(
        private val path: String,
        private val type: DocsFieldType,
        private var isOptional: Boolean = false,
        private var note: String? = null
    ) {
        private fun markOptional(): FieldSpec = apply {
            isOptional = true
            note = " (optional)"
        }

        private fun markRequired(): FieldSpec = apply {
            isOptional = false
            note = " (필수값)"
        }

        fun optional(): FieldSpec = markOptional()

        fun required(): FieldSpec = markRequired()

        val optional get() = markOptional()
        val required get() = markRequired()

        infix fun optional(@Suppress("UNUSED_PARAMETER") marker: Unit): FieldSpec = markOptional()

        infix fun required(@Suppress("UNUSED_PARAMETER") marker: Unit): FieldSpec = markRequired()

        infix fun optional(flag: Boolean): FieldSpec = if (flag) markOptional() else markRequired()

        infix fun optional(description: String) {
            markOptional().means(description)
        }

        infix fun required(description: String) {
            markRequired().means(description)
        }

        infix fun means(description: String) {
            val annotatedDescription = description + (note.orEmpty())
            val descriptor = fieldWithPath(path)
                .type(type.jsonType)
                .description(formatDescription(annotatedDescription, type))
            descriptors += if (isOptional) descriptor.optional() else descriptor
        }
    }

    fun build(): List<FieldDescriptor> = descriptors
}

fun fields(block: FieldDsl.() -> Unit): Array<FieldDescriptor> {
    val dsl = FieldDsl()
    dsl.block()
    return dsl.build().toTypedArray()
}

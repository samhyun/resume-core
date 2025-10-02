package com.resume.core.support.docs

import com.epages.restdocs.apispec.ParameterDescriptorWithType
import com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName
import com.epages.restdocs.apispec.SimpleType
import org.springframework.restdocs.payload.JsonFieldType

class ParameterDsl internal constructor() {
    private val descriptors = mutableListOf<ParameterDescriptorWithType>()

    infix fun String.type(type: DocsFieldType): ParameterSpec = ParameterSpec(this, type)

    inner class ParameterSpec internal constructor(
        private val name: String,
        private val type: DocsFieldType,
        private var isOptional: Boolean = false,
        private var note: String? = null
    ) {
        private fun markOptional(): ParameterSpec = apply {
            isOptional = true
            note = " (optional)"
        }

        private fun markRequired(): ParameterSpec = apply {
            isOptional = false
            note = " (필수값)"
        }

        fun optional(): ParameterSpec = markOptional()

        fun required(): ParameterSpec = markRequired()

        val optional get() = markOptional()

        val required get() = markRequired()

        infix fun optional(@Suppress("UNUSED_PARAMETER") marker: Unit): ParameterSpec = markOptional()

        infix fun required(@Suppress("UNUSED_PARAMETER") marker: Unit): ParameterSpec = markRequired()

        infix fun optional(flag: Boolean): ParameterSpec = if (flag) markOptional() else markRequired()

        infix fun means(description: String) {
            val annotatedDescription = formatDescription(description, type) + (note.orEmpty())
            val descriptor = parameterWithName(name)
                .type(type.toSimpleType())
                .description(annotatedDescription)
            descriptors += if (isOptional) descriptor.optional() else descriptor
        }
    }

    fun build(): List<ParameterDescriptorWithType> = descriptors
}

fun parameters(block: ParameterDsl.() -> Unit): Array<ParameterDescriptorWithType> {
    val dsl = ParameterDsl()
    dsl.block()
    return dsl.build().toTypedArray()
}

private fun DocsFieldType.toSimpleType(): SimpleType = when (jsonType) {
    JsonFieldType.BOOLEAN -> SimpleType.BOOLEAN
    JsonFieldType.NUMBER -> SimpleType.NUMBER
    JsonFieldType.NULL -> SimpleType.STRING
    JsonFieldType.OBJECT -> SimpleType.STRING
    JsonFieldType.ARRAY -> SimpleType.STRING
    JsonFieldType.VARIES -> SimpleType.STRING
    else -> SimpleType.STRING
}

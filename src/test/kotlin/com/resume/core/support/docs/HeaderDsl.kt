package com.resume.core.support.docs

import com.epages.restdocs.apispec.HeaderDescriptorWithType
import com.epages.restdocs.apispec.ResourceDocumentation.headerWithName

class HeaderDsl internal constructor() {
    private val specs = mutableListOf<HeaderSpec>()

    infix fun String.header(description: String): HeaderSpec =
        HeaderSpec(this, description).also { specs += it }

    inner class HeaderSpec internal constructor(
        private val name: String,
        private val description: String,
        private var isOptional: Boolean = false,
        private var note: String? = null
    ) {
        private fun markOptional(): HeaderSpec = apply {
            isOptional = true
            note = " (optional)"
        }

        private fun markRequired(): HeaderSpec = apply {
            isOptional = false
            note = " (필수값)"
        }

        fun optional(): HeaderSpec = markOptional()

        fun required(): HeaderSpec = markRequired()

        val optional get() = markOptional()

        val required get() = markRequired()

        infix fun optional(@Suppress("UNUSED_PARAMETER") marker: Unit): HeaderSpec = markOptional()

        infix fun required(@Suppress("UNUSED_PARAMETER") marker: Unit): HeaderSpec = markRequired()

        infix fun optional(flag: Boolean): HeaderSpec = if (flag) markOptional() else markRequired()

        fun toDescriptor(): HeaderDescriptorWithType = headerWithName(name)
            .description(description + (note.orEmpty()))
            .let { if (isOptional) it.optional() else it }
    }

    fun build(): List<HeaderDescriptorWithType> = specs.map { it.toDescriptor() }
}

fun headers(block: HeaderDsl.() -> Unit): Array<HeaderDescriptorWithType> {
    val dsl = HeaderDsl()
    dsl.block()
    return dsl.build().toTypedArray()
}

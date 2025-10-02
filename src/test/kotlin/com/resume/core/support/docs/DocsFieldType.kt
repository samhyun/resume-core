package com.resume.core.support.docs

import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation
import kotlin.reflect.KClass

sealed class DocsFieldType(
    val jsonType: JsonFieldType,
    val format: String? = null,
    val example: String? = null,
    val enumValues: List<String>? = null
) {
    object ARRAY : DocsFieldType(JsonFieldType.ARRAY)
    object BOOLEAN : DocsFieldType(JsonFieldType.BOOLEAN)
    object OBJECT : DocsFieldType(JsonFieldType.OBJECT)
    object NUMBER : DocsFieldType(JsonFieldType.NUMBER)
    object NULL : DocsFieldType(JsonFieldType.NULL)
    object STRING : DocsFieldType(JsonFieldType.STRING)
    object ANY : DocsFieldType(JsonFieldType.VARIES)
    object DATE : DocsFieldType(JsonFieldType.STRING, format = "date")
    object DATETIME : DocsFieldType(JsonFieldType.STRING, format = "date-time")

    class ENUM<T : Enum<T>>(enumClass: KClass<T>) : DocsFieldType(
        JsonFieldType.STRING,
        enumValues = enumClass.java.enumConstants.map { it.name }
    )
}

fun formatDescription(base: String, docsFieldType: DocsFieldType): String {
    val builder = StringBuilder(base)
    docsFieldType.format?.let { builder.append(" (format: $it)") }
    docsFieldType.enumValues?.let { builder.append(" (enum: ${it.joinToString(", ")})") }
    docsFieldType.example?.let { builder.append(" (example: $it)") }
    return builder.toString()
}

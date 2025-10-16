package com.resume.core.support.docs

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Paths

/**
 * REST Docs로 생성된 resource.json 파일을 보강하는 유틸리티
 */
object ResourceEnricher {

    private val objectMapper = jacksonObjectMapper()

    fun enrich(snippetName: String, block: EnrichmentContext.() -> Unit) {
        val resourcePath = Paths.get("build/generated-snippets/$snippetName/resource.json")
        if (!Files.exists(resourcePath)) {
            return
        }

        val root = (objectMapper.readTree(resourcePath.toFile()) as? ObjectNode) ?: return
        val context = EnrichmentContext(root)
        context.block()

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(resourcePath.toFile(), root)
    }
}

class EnrichmentContext(private val root: ObjectNode) {

    private val objectMapper = jacksonObjectMapper()

    fun request(block: RequestContext.() -> Unit) {
        val request = root.with("request")
        val context = RequestContext(request, objectMapper)
        context.block()
    }

    fun response(block: ResponseContext.() -> Unit) {
        val response = root.with("response")
        val context = ResponseContext(response, objectMapper)
        context.block()
    }
}

class RequestContext(
    private val request: ObjectNode,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {

    fun contentType(type: String) {
        request.put("contentType", type)
    }

    fun formParameters(block: FormParametersBuilder.() -> Unit) {
        val builder = FormParametersBuilder(objectMapper)
        builder.block()
        request.set<ArrayNode>("formParameters", builder.build())
    }

    fun requestFields(block: RequestFieldsBuilder.() -> Unit) {
        val builder = RequestFieldsBuilder(objectMapper)
        builder.block()
        request.set<ArrayNode>("requestFields", builder.build())
    }
}

class ResponseContext(
    private val response: ObjectNode,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
) {

    fun responseFields(block: ResponseFieldsBuilder.() -> Unit) {
        val builder = ResponseFieldsBuilder(objectMapper)
        builder.block()
        response.set<ArrayNode>("responseFields", builder.build())
    }
}

class FormParametersBuilder(private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper) {
    private val parameters = mutableListOf<ObjectNode>()

    fun parameter(
        name: String,
        type: String = "STRING",
        description: String,
        optional: Boolean = false,
        format: String? = null
    ) {
        parameters += objectMapper.createObjectNode().apply {
            put("name", name)
            set<ObjectNode>("attributes", objectMapper.createObjectNode().apply {
                format?.let { put("format", it) }
            })
            put("description", if (optional) "$description (optional)" else description)
            put("type", type)
            put("optional", optional)
            putNull("example")
            putNull("default")
        }
    }

    fun build(): ArrayNode = objectMapper.createArrayNode().apply {
        parameters.forEach { add(it) }
    }
}

class RequestFieldsBuilder(private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper) {
    private val fields = mutableListOf<ObjectNode>()

    fun field(
        path: String,
        type: String = "STRING",
        description: String,
        optional: Boolean = false,
        format: String? = null
    ) {
        fields += objectMapper.createObjectNode().apply {
            set<ObjectNode>("attributes", objectMapper.createObjectNode().apply {
                format?.let { put("format", it) }
            })
            put("description", if (optional) "$description (optional)" else description)
            put("ignored", false)
            put("path", path)
            put("type", type)
            put("optional", optional)
        }
    }

    fun build(): ArrayNode = objectMapper.createArrayNode().apply {
        fields.forEach { add(it) }
    }
}

class ResponseFieldsBuilder(private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper) {
    private val fields = mutableListOf<ObjectNode>()

    fun field(
        path: String,
        description: String,
        type: String = "STRING",
        optional: Boolean = true
    ) {
        fields += objectMapper.createObjectNode().apply {
            set<ObjectNode>("attributes", objectMapper.createObjectNode())
            put("description", if (optional) "$description (optional)" else description)
            put("ignored", false)
            put("path", path)
            put("type", type)
            put("optional", optional)
        }
    }

    fun build(): ArrayNode = objectMapper.createArrayNode().apply {
        fields.forEach { add(it) }
    }
}

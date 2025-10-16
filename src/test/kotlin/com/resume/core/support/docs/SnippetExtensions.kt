package com.resume.core.support.docs

import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder
import org.springframework.restdocs.request.RequestPartDescriptor

fun ResourceSnippetParametersBuilder.requestFields(block: FieldDsl.() -> Unit): ResourceSnippetParametersBuilder =
    requestFields(*fields(block))

fun ResourceSnippetParametersBuilder.responseFields(block: FieldDsl.() -> Unit): ResourceSnippetParametersBuilder =
    responseFields(*fields(block))

fun ResourceSnippetParametersBuilder.requestHeaders(block: HeaderDsl.() -> Unit): ResourceSnippetParametersBuilder =
    requestHeaders(*headers(block))

fun ResourceSnippetParametersBuilder.requestParameters(block: ParameterDsl.() -> Unit): ResourceSnippetParametersBuilder =
    queryParameters(*parameters(block))

fun requestParts(block: PartDsl.() -> Unit): Array<RequestPartDescriptor> =
    parts(block)

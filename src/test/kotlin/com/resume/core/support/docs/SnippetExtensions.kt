package com.resume.core.support.docs

import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder

fun ResourceSnippetParametersBuilder.requestFields(block: FieldDsl.() -> Unit): ResourceSnippetParametersBuilder =
    requestFields(*fields(block))

fun ResourceSnippetParametersBuilder.responseFields(block: FieldDsl.() -> Unit): ResourceSnippetParametersBuilder =
    responseFields(*fields(block))

fun ResourceSnippetParametersBuilder.requestHeaders(block: HeaderDsl.() -> Unit): ResourceSnippetParametersBuilder =
    requestHeaders(*headers(block))

fun ResourceSnippetParametersBuilder.requestParameters(block: ParameterDsl.() -> Unit): ResourceSnippetParametersBuilder =
    queryParameters(*parameters(block))

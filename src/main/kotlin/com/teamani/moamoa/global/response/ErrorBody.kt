package com.teamani.moamoa.global.response

import com.fasterxml.jackson.annotation.JsonInclude

data class ErrorBody(
    val code: String,
    val message: String,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val details: List<ValidationDetail>? = null,
)

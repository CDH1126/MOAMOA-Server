package com.teamani.moamoa.global.response

data class ValidationDetail(
    val field: String,
    val reason: String,
    val rejectedValue: Any?,
)

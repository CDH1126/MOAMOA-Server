package com.teamani.moamoa.global.response

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.OffsetDateTime

// 실패 응답 형식
// success=false, data=null, error!=null 형태 고정
@JsonPropertyOrder("success", "data", "error", "timestamp")
data class ErrorResponse(
    val success: Boolean = false,
    val data: Any? = null,
    val error: ErrorBody,
    val timestamp: OffsetDateTime = currentTimestamp(),
)

package com.teamani.moamoa.global.response

import java.time.OffsetDateTime

// 실패 응답 형식
// success=false, data=null, error!=null 형태 고정
data class ErrorResponse(
    val success: Boolean = false,
    val data: Any? = null,
    val error: ErrorBody,
    val timestamp: OffsetDateTime = currentTimestamp(),
)

package com.teamani.moamoa.global.response

import java.time.OffsetDateTime

// 성공 응답 형식
// success=true, data!=null, error=null 형태 고정
data class CommonResponse<T : Any>(
    val success: Boolean = true,
    val data: T,
    val error: Any? = null,
    val timestamp: OffsetDateTime = currentTimestamp(),
)

package com.teamani.moamoa.global.response

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.time.OffsetDateTime

// 성공 응답 형식
// success=true, data!=null, error=null 형태 고정
@JsonPropertyOrder("success", "data", "error", "timestamp")
data class CommonResponse<T : Any>(
    val success: Boolean = true,
    val data: T,
    val error: Any? = null,
    val timestamp: OffsetDateTime = currentTimestamp(),
) {
    companion object {
        fun <T : Any> success(data: T): CommonResponse<T> = CommonResponse(data = data)
    }
}

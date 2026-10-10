package com.teamani.moamoa.global.exception

import com.teamani.moamoa.global.response.ErrorBody
import com.teamani.moamoa.global.response.ErrorResponse
import com.teamani.moamoa.global.response.ValidationDetail

// 실패 응답 body 생성의 단일 진입점
// message가 없으면 ErrorCode 기본 메시지 사용
object ErrorResponseFactory {

    fun of(
        errorCode: ErrorCode,
        message: String? = null,
        details: List<ValidationDetail>? = null,
    ): ErrorResponse {
        return ErrorResponse(
            error = ErrorBody(
                code = errorCode.code,
                message = message ?: errorCode.message,
                details = details,
            ),
        )
    }
}

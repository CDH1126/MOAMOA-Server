package com.teamani.moamoa.global.exception

// 비즈니스 규칙 위반 시 Service에서 던지는 예외
// HTTP 응답 변환은 GlobalExceptionHandler에서 처리
open class ApiException(
    val errorCode: ErrorCode,
    override val message: String? = errorCode.message,
) : RuntimeException(message)

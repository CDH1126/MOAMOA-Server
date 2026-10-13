package com.teamani.moamoa.global.exception

import org.springframework.http.HttpStatus

// Team-AnI 공통 예외 처리 가이드 v1 기준 에러 코드 중 현재 기능에 필요한 코드만 정의
// code는 프론트엔드와의 계약이므로 값 변경 금지
enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청값 검증에 실패했습니다."),
    INPUT_ERROR(HttpStatus.BAD_REQUEST, "INPUT_ERROR", "요청값 형식이 올바르지 않습니다."),
    JSON_PARSE_ERROR(HttpStatus.BAD_REQUEST, "JSON_PARSE_ERROR", "요청 본문(JSON) 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요하거나 토큰이 유효하지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "요청을 수행할 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "리소스 충돌이 발생했습니다."),
    UNPROCESSABLE_ENTITY(HttpStatus.UNPROCESSABLE_CONTENT, "UNPROCESSABLE_ENTITY", "현재 상태에서는 요청을 처리할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다."),
}

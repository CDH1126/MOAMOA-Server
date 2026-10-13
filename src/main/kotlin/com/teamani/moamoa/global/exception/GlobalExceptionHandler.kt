package com.teamani.moamoa.global.exception

import com.teamani.moamoa.global.response.ErrorResponse
import com.teamani.moamoa.global.response.ValidationDetail
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(
        ex: ApiException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logError(ex.errorCode, ex, request)
        return ResponseEntity.status(ex.errorCode.status)
            .body(ErrorResponseFactory.of(ex.errorCode, ex.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logError(ErrorCode.VALIDATION_ERROR, ex, request)
        val details = ex.bindingResult.fieldErrors.map {
            ValidationDetail(
                field = it.field,
                reason = it.defaultMessage.orEmpty(),
                rejectedValue = it.rejectedValue,
            )
        }
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.status)
            .body(ErrorResponseFactory.of(ErrorCode.VALIDATION_ERROR, details = details))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logError(ErrorCode.JSON_PARSE_ERROR, ex, request)
        return ResponseEntity.status(ErrorCode.JSON_PARSE_ERROR.status)
            .body(ErrorResponseFactory.of(ErrorCode.JSON_PARSE_ERROR))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logError(ErrorCode.INPUT_ERROR, ex, request)
        return ResponseEntity.status(ErrorCode.INPUT_ERROR.status)
            .body(ErrorResponseFactory.of(ErrorCode.INPUT_ERROR))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logError(ErrorCode.INTERNAL_ERROR, ex, request)
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status)
            .body(ErrorResponseFactory.of(ErrorCode.INTERNAL_ERROR))
    }

    // 4xx는 warn, 5xx는 stack trace와 함께 error 로그 기록
    // 요청값은 민감 정보 포함 가능성으로 로그 제외
    private fun logError(errorCode: ErrorCode, ex: Exception, request: HttpServletRequest) {
        val status = errorCode.status.value()
        if (errorCode.status.is5xxServerError) {
            log.error(LOG_FORMAT, status, errorCode.code, request.method, request.requestURI, ex.javaClass.simpleName, ex)
        } else {
            log.warn(LOG_FORMAT, status, errorCode.code, request.method, request.requestURI, ex.javaClass.simpleName)
        }
    }

    companion object {
        private const val LOG_FORMAT = "[{} {}] {} {} - {}"
    }
}

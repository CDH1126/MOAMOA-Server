package com.teamani.moamoa.notice.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.Optional

data class NoticeCreateRequest(
    @field:NotBlank(message = "공지 제목은 필수입니다.")
    @field:Size(max = 50, message = "제목은 50자 이하여야 합니다.")
    val title: String?,

    @field:NotBlank(message = "공지 내용은 필수입니다.")
    val content: String?
)

data class NoticeCreateResponse(
    val noticeId: Long,
    val title: String,
)

data class NoticeDeleteResponse(
    val noticeId: Long
)

data class NoticeDetailResponse(
    val noticeId: Long,
    val title: String,
    val content: String,
    val createdAt: OffsetDateTime
)

data class NoticeSummaryResponse(
    val noticeId: Long,
    val title: String,
    val createdAt: OffsetDateTime
)

data class NoticeListResponse(
    val notices: List<NoticeSummaryResponse>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean
)

// 부분 수정: 미전송은 null(유지), null 전송은 Optional.empty, 값 전송은 Optional.of
// 제목·내용은 필수 항목이라 null 전송 시 400 VALIDATION_ERROR (검증은 타입 인자에 작성)
data class NoticeUpdateRequest(
    val title: Optional<
        @NotNull(message = "공지 제목은 비울 수 없습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "공지 제목은 공백일 수 없습니다.")
        @Size(max = 50, message = "제목은 50자 이하여야 합니다.")
        String
    >? = null,

    val content: Optional<
        @NotNull(message = "공지 내용은 비울 수 없습니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "공지 내용은 공백일 수 없습니다.")
        String
    >? = null
)

data class NoticeUpdateResponse(
    val noticeId: Long,
    val title: String,
    val content: String
)

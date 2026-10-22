package com.teamani.moamoa.meeting.dto

import com.teamani.moamoa.member.entity.MemberRole
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.Optional

data class MeetingCreateRequest(
    @field:NotBlank(message = "모임명은 필수입니다.")
    @field:Size(max = 20, message = "모임명은 20자 이하여야 합니다.")
    val name: String?,

    val description: String? = null
)

data class MeetingCreateResponse(
    val meetingId: Long,
    val name: String,
    val inviteCode: String,
    val myRole: MemberRole
)

data class MyMeetingListResponse(
    val meetings: List<MyMeetingSummaryResponse>
)

data class MyMeetingSummaryResponse(
    val meetingId: Long,
    val name: String,
    val memberCount: Long,
    val myRole: MemberRole
)

// 초대코드는 공백 없이 대소문자까지 정확히 일치해야 함 (trim·대소문자 변환 없음)
data class MeetingJoinRequest(
    @field:NotBlank(message = "초대코드는 필수입니다.")
    @field:Pattern(regexp = "\\S*", message = "초대코드에 공백을 포함할 수 없습니다.")
    val inviteCode: String?
)

data class MeetingJoinResponse(
    val meetingId: Long,
    val name: String,
    val myRole: MemberRole
)

data class MeetingDetailResponse(
    val meetingId: Long,
    val name: String,
    val description: String?,
    val memberCount: Long,
    val myRole: MemberRole
)

// 부분 수정: 미전송은 null(유지), null 전송은 Optional.empty, 값 전송은 Optional.of
// Optional 내부 값 검증은 타입 인자에 작성 (@field로 Optional에 달면 검증기를 찾지 못해 500)
data class MeetingUpdateRequest(
    // 필수 항목: null 전송 시 400 VALIDATION_ERROR
    val name: Optional<
        @NotNull(message = "모임명은 필수입니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "모임명은 공백일 수 없습니다.")
        @Size(max = 20, message = "모임명은 20자 이하여야 합니다.")
        String
    >? = null,

    // 선택 항목: null 전송 시 비우기
    val description: Optional<String>? = null
)

data class MeetingUpdateResponse(
    val meetingId: Long,
    val name: String,
    val description: String?
)

data class MeetingDeleteResponse(
    val meetingId: Long
)

data class MeetingInviteCodeResponse(
    val inviteCode: String
)

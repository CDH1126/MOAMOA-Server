package com.teamani.moamoa.meeting.service

import com.teamani.moamoa.global.exception.ApiException
import com.teamani.moamoa.global.exception.ErrorCode
import com.teamani.moamoa.meeting.dto.MeetingCreateRequest
import com.teamani.moamoa.meeting.dto.MeetingCreateResponse
import com.teamani.moamoa.meeting.dto.MeetingDeleteResponse
import com.teamani.moamoa.meeting.dto.MeetingDetailResponse
import com.teamani.moamoa.meeting.dto.MeetingInviteCodeResponse
import com.teamani.moamoa.meeting.dto.MeetingJoinRequest
import com.teamani.moamoa.meeting.dto.MeetingJoinResponse
import com.teamani.moamoa.meeting.dto.MeetingUpdateRequest
import com.teamani.moamoa.meeting.dto.MeetingUpdateResponse
import com.teamani.moamoa.meeting.dto.MyMeetingListResponse
import com.teamani.moamoa.meeting.dto.MyMeetingSummaryResponse
import com.teamani.moamoa.meeting.entity.Meeting
import com.teamani.moamoa.meeting.repository.MeetingRepository
import com.teamani.moamoa.member.entity.Member
import com.teamani.moamoa.member.entity.MemberRole
import com.teamani.moamoa.member.repository.MemberRepository
import com.teamani.moamoa.member.service.MemberService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class MeetingService(
    private val meetingRepository: MeetingRepository,
    private val memberRepository: MemberRepository,
    private val memberService: MemberService
) {

    @Transactional
    fun createMeeting(userId: Long, request: MeetingCreateRequest): MeetingCreateResponse {
        val meeting = meetingRepository.save(
            Meeting(
                name = requireNotNull(request.name) { "모임명은 필수입니다." },
                description = request.description,
                inviteCode = generateInviteCode()
            )
        )
        val meetingId = checkNotNull(meeting.id) { "모임 식별자가 생성되지 않았습니다." }

        // 모임 생성자를 관리자(ADMIN)로 등록
        memberRepository.save(Member(meetingId = meetingId, userId = userId, role = MemberRole.ADMIN))

        return MeetingCreateResponse(
            meetingId = meetingId,
            name = meeting.name,
            inviteCode = meeting.inviteCode,
            myRole = MemberRole.ADMIN
        )
    }

    @Transactional(readOnly = true)
    fun getMyMeetings(userId: Long): MyMeetingListResponse {
        val myMembers = memberRepository.findAllByUserIdOrderByIdAsc(userId)
        if (myMembers.isEmpty()) {
            return MyMeetingListResponse(meetings = emptyList())
        }

        val meetings = meetingRepository.findAllByIdInAndEndedAtIsNull(myMembers.map { it.meetingId })
            .associateBy { checkNotNull(it.id) { "모임 식별자가 없습니다." } }
        if (meetings.isEmpty()) {
            return MyMeetingListResponse(meetings = emptyList())
        }

        val memberCounts = memberRepository.countGroupByMeetingIdIn(meetings.keys)
            .associate { it.meetingId to it.memberCount }

        // 참여 순서 유지, 삭제(종료)된 모임 제외
        val summaries = myMembers.mapNotNull { member ->
            meetings[member.meetingId]?.let { meeting ->
                MyMeetingSummaryResponse(
                    meetingId = member.meetingId,
                    name = meeting.name,
                    memberCount = memberCounts[member.meetingId] ?: 0,
                    myRole = member.role
                )
            }
        }
        return MyMeetingListResponse(meetings = summaries)
    }

    @Transactional
    fun joinMeeting(userId: Long, request: MeetingJoinRequest): MeetingJoinResponse {
        val inviteCode = requireNotNull(request.inviteCode) { "초대코드는 필수입니다." }
        val meeting = meetingRepository.findByInviteCodeAndEndedAtIsNull(inviteCode)
            ?: throw ApiException(ErrorCode.NOT_FOUND, "초대코드에 해당하는 모임을 찾을 수 없습니다.")
        val meetingId = checkNotNull(meeting.id) { "모임 식별자가 없습니다." }

        if (memberRepository.existsByMeetingIdAndUserId(meetingId, userId)) {
            throw ApiException(ErrorCode.CONFLICT, ALREADY_JOINED_MESSAGE)
        }

        try {
            memberRepository.saveAndFlush(Member(meetingId = meetingId, userId = userId, role = MemberRole.MEMBER))
        } catch (ex: DataIntegrityViolationException) {
            // 중복 확인 직후 동시 요청으로 unique 제약 위반 시 409 응답
            throw ApiException(ErrorCode.CONFLICT, ALREADY_JOINED_MESSAGE)
        }

        return MeetingJoinResponse(
            meetingId = meetingId,
            name = meeting.name,
            myRole = MemberRole.MEMBER
        )
    }

    @Transactional(readOnly = true)
    fun getMeeting(meetingId: Long, userId: Long): MeetingDetailResponse {
        val member = memberService.validateMember(meetingId, userId)
        val meeting = findActiveMeeting(meetingId)

        return MeetingDetailResponse(
            meetingId = meetingId,
            name = meeting.name,
            description = meeting.description,
            memberCount = memberRepository.countByMeetingId(meetingId),
            myRole = member.role
        )
    }

    @Transactional
    fun updateMeeting(meetingId: Long, userId: Long, request: MeetingUpdateRequest): MeetingUpdateResponse {
        memberService.validateAdmin(meetingId, userId)
        val meeting = findActiveMeeting(meetingId)

        meeting.update(name = request.name, description = request.description)

        return MeetingUpdateResponse(
            meetingId = meetingId,
            name = meeting.name,
            description = meeting.description
        )
    }

    @Transactional
    fun deleteMeeting(meetingId: Long, userId: Long): MeetingDeleteResponse {
        memberService.validateAdmin(meetingId, userId)
        val meeting = findActiveMeeting(meetingId)

        meeting.end()

        return MeetingDeleteResponse(meetingId = meetingId)
    }

    @Transactional(readOnly = true)
    fun getInviteCode(meetingId: Long, userId: Long): MeetingInviteCodeResponse {
        memberService.validateAdmin(meetingId, userId)
        val meeting = findActiveMeeting(meetingId)

        return MeetingInviteCodeResponse(inviteCode = meeting.inviteCode)
    }

    private fun findActiveMeeting(meetingId: Long): Meeting =
        meetingRepository.findByIdAndEndedAtIsNull(meetingId)
            ?: throw ApiException(ErrorCode.NOT_FOUND, MemberService.MEETING_NOT_FOUND_MESSAGE)

    // 대문자·숫자 8자리 초대코드 생성 (기존 코드와 중복 시 재생성)
    private fun generateInviteCode(): String {
        repeat(INVITE_CODE_MAX_ATTEMPTS) {
            val code = (1..INVITE_CODE_LENGTH)
                .map { INVITE_CODE_CHARS[secureRandom.nextInt(INVITE_CODE_CHARS.length)] }
                .joinToString("")
            if (!meetingRepository.existsByInviteCode(code)) {
                return code
            }
        }
        throw IllegalStateException("초대코드 생성에 실패했습니다.")
    }

    companion object {
        private const val ALREADY_JOINED_MESSAGE = "이미 참여한 모임입니다."
        private const val INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        private const val INVITE_CODE_LENGTH = 8
        private const val INVITE_CODE_MAX_ATTEMPTS = 10

        // 초대코드 추측 방지를 위한 보안 난수 생성기
        private val secureRandom = SecureRandom()
    }
}

package com.teamani.moamoa.schedule.service

import com.teamani.moamoa.schedule.dto.ScheduleCreateRequest
import com.teamani.moamoa.schedule.dto.ScheduleCreateResponse
import com.teamani.moamoa.schedule.dto.ScheduleListResponse
import com.teamani.moamoa.schedule.dto.ScheduleSummaryResponse
import com.teamani.moamoa.schedule.entity.Schedule
import com.teamani.moamoa.schedule.repository.ScheduleRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository
) {

    @Transactional
    fun createSchedule(meetingId: Long, userId: Long, request: ScheduleCreateRequest): ScheduleCreateResponse {
        // 모임 존재 여부 및 ADMIN 권한 검증 (Meeting·Member 도메인 연동 후속 이슈 처리 예정)
        validateMeeting(meetingId)
        validateAdmin(meetingId, userId)

        val schedule = Schedule(
            meetingId = meetingId,
            title = requireNotNull(request.title) { "제목은 필수입니다." },
            description = request.description,
            startsAt = requireNotNull(request.startAt) { "시작 일시는 필수입니다." },
            endedAt = request.endAt,
            location = request.location
        )

        val savedSchedule = scheduleRepository.save(schedule)

        return ScheduleCreateResponse(
            scheduleId = checkNotNull(savedSchedule.id) { "일정 식별자가 생성되지 않았습니다." },
            title = savedSchedule.title,
            startAt = savedSchedule.startsAt
        )
    }

    @Transactional(readOnly = true)
    fun getSchedules(meetingId: Long, userId: Long, from: LocalDate, to: LocalDate): ScheduleListResponse {
        validateMeeting(meetingId)
        validateMember(meetingId, userId)

        if (from.isAfter(to)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 시작일은 종료일보다 이후일 수 없습니다.")
        }

        // from, to 모두 포함하도록 KST 기준 [from 00:00, to 다음 날 00:00) 범위로 조회
        val schedules = scheduleRepository.findAllByMeetingIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            meetingId,
            from.atStartOfDay().atOffset(KST),
            to.plusDays(1).atStartOfDay().atOffset(KST)
        )

        return ScheduleListResponse(
            schedules = schedules.map { schedule ->
                ScheduleSummaryResponse(
                    scheduleId = checkNotNull(schedule.id) { "일정 식별자가 없습니다." },
                    title = schedule.title,
                    startAt = schedule.startsAt.withOffsetSameInstant(KST),
                    endAt = schedule.endedAt?.withOffsetSameInstant(KST),
                    location = schedule.location
                )
            }
        )
    }

    // 모임 존재 여부 검증 함수 (추후 Meeting 도메인 연동 자리)
    private fun validateMeeting(meetingId: Long) {
        // TODO: meetingRepository로 모임을 조회하고, 없으면 404 NOT_FOUND 예외를 throw하도록 연결
    }

    // 관리자 권한 검증 함수 (추후 Member 도메인 연동 자리)
    private fun validateAdmin(meetingId: Long, userId: Long) {
        // TODO: 해당 사용자가 모임의 ADMIN인지 확인하고, 아니면 403 FORBIDDEN 예외를 throw하도록 연결
    }

    // 모임 구성원 권한 검증 함수 (추후 Member 도메인 연동 자리)
    private fun validateMember(meetingId: Long, userId: Long) {
        // TODO: 해당 사용자가 모임의 구성원인지 확인하고, 아니면 403 FORBIDDEN 예외를 throw하도록 연결
    }

    companion object {
        // API 명세상 응답 일시와 날짜 조회 기준은 KST(+09:00)
        private val KST: ZoneOffset = ZoneOffset.ofHours(9)
    }
}

package com.teamani.moamoa.schedule.repository

import com.teamani.moamoa.schedule.entity.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface ScheduleRepository : JpaRepository<Schedule, Long> {

    // 시작 일시가 [startInclusive, endExclusive) 범위인 모임 일정을 시작 일시 순으로 조회
    fun findAllByMeetingIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
        meetingId: Long,
        startInclusive: OffsetDateTime,
        endExclusive: OffsetDateTime
    ): List<Schedule>
}

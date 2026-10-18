package com.teamani.moamoa.meeting.repository

import com.teamani.moamoa.meeting.entity.Meeting
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingRepository : JpaRepository<Meeting, Long> {

    // 삭제(종료)되지 않은 모임 조회
    fun findByIdAndEndedAtIsNull(id: Long): Meeting?

    fun existsByIdAndEndedAtIsNull(id: Long): Boolean

    fun findByInviteCodeAndEndedAtIsNull(inviteCode: String): Meeting?

    fun findAllByIdInAndEndedAtIsNull(ids: Collection<Long>): List<Meeting>

    // 초대코드 중복 확인 (삭제된 모임 포함, DB unique 제약 기준)
    fun existsByInviteCode(inviteCode: String): Boolean
}

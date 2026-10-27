package com.teamani.moamoa.member.repository

import com.teamani.moamoa.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MemberRepository : JpaRepository<Member, Long> {

    fun findByMeetingIdAndUserId(meetingId: Long, userId: Long): Member?

    fun existsByMeetingIdAndUserId(meetingId: Long, userId: Long): Boolean

    fun findAllByUserIdOrderByIdAsc(userId: Long): List<Member>

    fun findAllByMeetingIdOrderByIdAsc(meetingId: Long): List<Member>

    fun countByMeetingId(meetingId: Long): Long

    // 모임별 구성원 수를 한 번의 집계 쿼리로 조회 (모임 수만큼 count 쿼리가 반복되는 N+1 방지)
    @Query(
        "select m.meetingId as meetingId, count(m) as memberCount " +
            "from Member m where m.meetingId in :meetingIds group by m.meetingId",
    )
    fun countGroupByMeetingIdIn(@Param("meetingIds") meetingIds: Collection<Long>): List<MeetingMemberCount>
}

// 모임별 구성원 수 집계 결과
interface MeetingMemberCount {
    val meetingId: Long
    val memberCount: Long
}

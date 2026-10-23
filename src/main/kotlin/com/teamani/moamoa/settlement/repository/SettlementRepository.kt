package com.teamani.moamoa.settlement.repository

import com.teamani.moamoa.settlement.entity.Settlement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface SettlementRepository : JpaRepository<Settlement, Long> {

    // 모임의 정산을 최신순으로 조회 (생성 일시가 같으면 나중에 만든 정산 먼저)
    fun findAllByMeetingIdOrderByCreatedAtDescIdDesc(meetingId: Long): List<Settlement>

    // 다른 모임의 정산은 조회되지 않도록 모임 ID까지 함께 조건으로 사용
    fun findByIdAndMeetingId(id: Long, meetingId: Long): Settlement?

    // 정기 회비 삭제 전 기존 회차 정산과의 연결만 해제 (정산·납부 내역은 유지)
    @Modifying
    @Query("update Settlement s set s.regularDuesId = null where s.regularDuesId = :regularDuesId")
    fun clearRegularDuesId(regularDuesId: Long): Int
}

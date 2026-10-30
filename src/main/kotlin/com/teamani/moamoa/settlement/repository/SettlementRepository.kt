package com.teamani.moamoa.settlement.repository

import com.teamani.moamoa.settlement.entity.Settlement
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface SettlementRepository : JpaRepository<Settlement, Long> {

    // 모임의 정산을 최신순으로 조회 (생성 일시가 같으면 나중에 만든 정산 먼저)
    fun findAllByMeetingIdOrderByCreatedAtDescIdDesc(meetingId: Long): List<Settlement>

    // 다른 모임의 정산은 조회되지 않도록 모임 ID까지 함께 조건으로 사용
    fun findByIdAndMeetingId(id: Long, meetingId: Long): Settlement?

    // 납부 제출·승인·반려를 정산 단위로 직렬화 (트랜잭션이 끝날 때까지 같은 정산의 다른 요청은 대기)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Settlement s where s.id = :id and s.meetingId = :meetingId")
    fun findByIdAndMeetingIdForUpdate(id: Long, meetingId: Long): Settlement?
}

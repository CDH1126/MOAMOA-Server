package com.teamani.moamoa.schedule.entity

// 일정 상태
enum class ScheduleStatus {
    // 관리자가 시간을 정해 직접 등록한 일정 (일정 생성 API 기본값)
    SCHEDULED,

    // 투표 진행 중인 일정 (시작 일시 미정, 투표 생성 API에서 생성)
    VOTING,

    // 투표로 시간이 확정된 일정 (투표 확정 API에서 전환)
    CONFIRMED,

    // 취소된 일정 (일정 취소 API에서 전환)
    CANCELED
}

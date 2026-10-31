package com.teamani.moamoa.settlement.entity

// 정산 진행 상태 (대상자 전원이 PAID가 되면 COMPLETED)
enum class SettlementStatus {
    IN_PROGRESS,
    COMPLETED
}

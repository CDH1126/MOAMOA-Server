package com.teamani.moamoa.settlement.controller

import com.teamani.moamoa.global.response.CommonResponse
import com.teamani.moamoa.settlement.dto.SettlementCreateRequest
import com.teamani.moamoa.settlement.dto.SettlementCreateResponse
import com.teamani.moamoa.settlement.dto.SettlementDetailResponse
import com.teamani.moamoa.settlement.dto.SettlementListResponse
import com.teamani.moamoa.settlement.service.SettlementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/meetings/{meetingId}/settlements")
class SettlementController(
    private val settlementService: SettlementService
) {

    @PostMapping
    fun createSettlement(
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: SettlementCreateRequest
    ): ResponseEntity<CommonResponse<SettlementCreateResponse>> {
        val currentMemberId = 1L // TODO: 인증·구성원 연동 시 실제 구성원 ID로 교체 (#50)

        val response = settlementService.createSettlement(meetingId, currentMemberId, request)

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse(data = response))
    }

    @GetMapping
    fun getSettlements(@PathVariable meetingId: Long): ResponseEntity<CommonResponse<SettlementListResponse>> {
        val currentMemberId = 1L // TODO: 인증·구성원 연동 시 실제 구성원 ID로 교체 (#50)

        return ResponseEntity.ok(CommonResponse(data = settlementService.getSettlements(meetingId, currentMemberId)))
    }

    @GetMapping("/{settlementId}")
    fun getSettlement(
        @PathVariable meetingId: Long,
        @PathVariable settlementId: Long
    ): ResponseEntity<CommonResponse<SettlementDetailResponse>> {
        val currentMemberId = 1L // TODO: 인증·구성원 연동 시 실제 구성원 ID로 교체 (#50)

        return ResponseEntity.ok(
            CommonResponse(data = settlementService.getSettlement(meetingId, currentMemberId, settlementId))
        )
    }
}

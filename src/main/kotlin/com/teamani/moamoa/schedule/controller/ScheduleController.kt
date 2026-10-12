package com.teamani.moamoa.schedule.controller

import com.teamani.moamoa.schedule.dto.ScheduleCreateRequest
import com.teamani.moamoa.schedule.dto.ScheduleCreateResponse
import com.teamani.moamoa.schedule.dto.ScheduleDetailResponse
import com.teamani.moamoa.schedule.dto.ScheduleListResponse
import com.teamani.moamoa.schedule.dto.ScheduleUpdateRequest
import com.teamani.moamoa.schedule.service.ScheduleService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
@RequestMapping("/v1/meetings/{meetingId}/schedules")
class ScheduleController(
    private val scheduleService: ScheduleService
) {

    @PostMapping
    fun createSchedule(
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: ScheduleCreateRequest
    ): ResponseEntity<Map<String, Any?>> {
        val currentUserId = 1L // TODO: 인증 구현 시 실제 사용자 ID로 교체

        val response: ScheduleCreateResponse = scheduleService.createSchedule(meetingId, currentUserId, request)

        // API 명세서 Envelope 포맷 규격: { success, data, error, timestamp }
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf(
                "success" to true,
                "data" to response,
                "error" to null,
                "timestamp" to OffsetDateTime.now()
            )
        )
    }

    @GetMapping
    fun getSchedules(
        @PathVariable meetingId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<Map<String, Any?>> {
        val currentUserId = 1L // TODO: 인증 구현 시 실제 사용자 ID로 교체

        val response: ScheduleListResponse = scheduleService.getSchedules(meetingId, currentUserId, from, to)

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "data" to response,
                "error" to null,
                "timestamp" to OffsetDateTime.now()
            )
        )
    }

    @GetMapping("/{scheduleId}")
    fun getSchedule(
        @PathVariable meetingId: Long,
        @PathVariable scheduleId: Long
    ): ResponseEntity<Map<String, Any?>> {
        val currentUserId = 1L // TODO: 인증 구현 시 실제 사용자 ID로 교체

        val response: ScheduleDetailResponse = scheduleService.getSchedule(meetingId, currentUserId, scheduleId)

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "data" to response,
                "error" to null,
                "timestamp" to OffsetDateTime.now()
            )
        )
    }

    @PatchMapping("/{scheduleId}")
    fun updateSchedule(
        @PathVariable meetingId: Long,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: ScheduleUpdateRequest
    ): ResponseEntity<Map<String, Any?>> {
        val currentUserId = 1L // TODO: 인증 구현 시 실제 사용자 ID로 교체

        val response: ScheduleDetailResponse =
            scheduleService.updateSchedule(meetingId, currentUserId, scheduleId, request)

        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "data" to response,
                "error" to null,
                "timestamp" to OffsetDateTime.now()
            )
        )
    }
}

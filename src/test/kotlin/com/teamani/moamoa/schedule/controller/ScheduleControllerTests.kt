package com.teamani.moamoa.schedule.controller

import com.teamani.moamoa.schedule.dto.ScheduleCreateRequest
import com.teamani.moamoa.schedule.dto.ScheduleCreateResponse
import com.teamani.moamoa.schedule.dto.ScheduleListResponse
import com.teamani.moamoa.schedule.dto.ScheduleSummaryResponse
import com.teamani.moamoa.schedule.service.ScheduleService
import io.kotest.core.spec.style.BehaviorSpec
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

class ScheduleControllerTests : BehaviorSpec({

    val scheduleService: ScheduleService = mock(ScheduleService::class.java)
    val scheduleController = ScheduleController(scheduleService)
    val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(scheduleController).build()

    // 요청 일시는 역직렬화 시 오프셋이 바뀌어도 equals가 유지되도록 UTC(Z)로 작성
    Given("유효한 일정 생성 요청(POST /v1/meetings/{meetingId}/schedules)이 주어졌을 때") {
        val meetingId = 1L
        val requestJson = """
            {
              "title": "10월 정기 러닝",
              "description": "반포 한강공원 집합",
              "startAt": "2026-10-10T22:00:00Z",
              "endAt": "2026-10-11T00:00:00Z",
              "location": "반포 한강공원"
            }
        """.trimIndent()
        val request = ScheduleCreateRequest(
            title = "10월 정기 러닝",
            description = "반포 한강공원 집합",
            startAt = OffsetDateTime.parse("2026-10-10T22:00:00Z"),
            endAt = OffsetDateTime.parse("2026-10-11T00:00:00Z"),
            location = "반포 한강공원"
        )
        val mockResponse = ScheduleCreateResponse(
            scheduleId = 20L,
            title = "10월 정기 러닝",
            startAt = OffsetDateTime.parse("2026-10-10T22:00:00Z")
        )

        `when`(scheduleService.createSchedule(meetingId, 1L, request)).thenReturn(mockResponse)

        When("API를 호출하면") {
            val resultActions = mockMvc.perform(
                post("/v1/meetings/$meetingId/schedules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )

            Then("201 Created 응답과 공통 Envelope 포맷이 반환된다") {
                resultActions
                    .andExpect(status().isCreated)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.scheduleId").value(20L))
                    .andExpect(jsonPath("$.data.title").value("10월 정기 러닝"))
                    .andExpect(jsonPath("$.data.startAt").exists())
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists())
            }
        }
    }

    Given("필수값이 누락되었거나 일시 순서가 잘못된 일정 생성 요청이 주어졌을 때") {
        val meetingId = 1L
        val invalidRequests = mapOf(
            "title 누락" to """{ "startAt": "2026-10-10T22:00:00Z" }""",
            "공백 title" to """{ "title": "   ", "startAt": "2026-10-10T22:00:00Z" }""",
            "startAt 누락" to """{ "title": "10월 정기 러닝" }""",
            "endAt이 startAt보다 이전" to """
                { "title": "10월 정기 러닝", "startAt": "2026-10-10T22:00:00Z", "endAt": "2026-10-10T21:00:00Z" }
            """.trimIndent()
        )

        invalidRequests.forEach { (case, requestJson) ->
            When("$case 요청으로 생성 API를 호출하면") {
                val resultActions = mockMvc.perform(
                    post("/v1/meetings/$meetingId/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
                )

                Then("400 Bad Request를 반환한다") {
                    resultActions.andExpect(status().isBadRequest)
                }
            }
        }
    }

    Given("유효한 일정 목록 조회 요청(GET /v1/meetings/{meetingId}/schedules)이 주어졌을 때") {
        val meetingId = 1L
        val from = LocalDate.parse("2026-10-01")
        val to = LocalDate.parse("2026-10-31")
        val mockResponse = ScheduleListResponse(
            schedules = listOf(
                ScheduleSummaryResponse(
                    scheduleId = 20L,
                    title = "10월 정기 러닝",
                    startAt = OffsetDateTime.parse("2026-10-11T07:00:00+09:00"),
                    endAt = OffsetDateTime.parse("2026-10-11T09:00:00+09:00"),
                    location = "반포 한강공원"
                )
            )
        )

        `when`(scheduleService.getSchedules(meetingId, 1L, from, to)).thenReturn(mockResponse)

        When("API를 호출하면") {
            val resultActions = mockMvc.perform(
                get("/v1/meetings/$meetingId/schedules")
                    .param("from", "2026-10-01")
                    .param("to", "2026-10-31")
            )

            Then("200 OK 응답과 공통 Envelope 포맷으로 일정 목록이 반환된다") {
                resultActions
                    .andExpect(status().isOk)
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.schedules[0].scheduleId").value(20L))
                    .andExpect(jsonPath("$.data.schedules[0].title").value("10월 정기 러닝"))
                    .andExpect(jsonPath("$.data.schedules[0].startAt").exists())
                    .andExpect(jsonPath("$.data.schedules[0].endAt").exists())
                    .andExpect(jsonPath("$.data.schedules[0].location").value("반포 한강공원"))
                    .andExpect(jsonPath("$.error").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists())
            }
        }
    }

    Given("from 또는 to가 누락되었거나 형식이 잘못된 일정 목록 조회 요청이 주어졌을 때") {
        val meetingId = 1L
        val invalidQueries = mapOf(
            "from 누락" to mapOf("to" to "2026-10-31"),
            "to 누락" to mapOf("from" to "2026-10-01"),
            "from 형식 오류" to mapOf("from" to "2026/10/01", "to" to "2026-10-31"),
            "to 형식 오류" to mapOf("from" to "2026-10-01", "to" to "20261031")
        )

        invalidQueries.forEach { (case, params) ->
            When("$case 요청으로 목록 API를 호출하면") {
                val request = get("/v1/meetings/$meetingId/schedules")
                params.forEach { (name, value) -> request.param(name, value) }
                val resultActions = mockMvc.perform(request)

                Then("400 Bad Request를 반환한다") {
                    resultActions.andExpect(status().isBadRequest)
                }
            }
        }
    }

    Given("from이 to보다 이후인 일정 목록 조회 요청이 주어졌을 때") {
        val meetingId = 1L
        val from = LocalDate.parse("2026-10-31")
        val to = LocalDate.parse("2026-10-01")

        `when`(scheduleService.getSchedules(meetingId, 1L, from, to))
            .thenThrow(ResponseStatusException(HttpStatus.BAD_REQUEST, "조회 시작일은 종료일보다 이후일 수 없습니다."))

        When("목록 API를 호출하면") {
            val resultActions = mockMvc.perform(
                get("/v1/meetings/$meetingId/schedules")
                    .param("from", "2026-10-31")
                    .param("to", "2026-10-01")
            )

            Then("400 Bad Request를 반환한다") {
                resultActions.andExpect(status().isBadRequest)
            }
        }
    }
})

package com.teamani.moamoa.schedule.service

import com.teamani.moamoa.schedule.dto.ScheduleCreateRequest
import com.teamani.moamoa.schedule.entity.Schedule
import com.teamani.moamoa.schedule.repository.ScheduleRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.OffsetDateTime

class ScheduleServiceTests : BehaviorSpec({

    val scheduleRepository: ScheduleRepository = mock(ScheduleRepository::class.java)
    val scheduleService = ScheduleService(scheduleRepository)

    afterEach { clearInvocations(scheduleRepository) }

    Given("모임 일정 생성 요청 데이터가 주어졌을 때") {
        val meetingId = 1L
        val userId = 1L
        val startAt = OffsetDateTime.parse("2026-10-11T07:00:00+09:00")
        val request = ScheduleCreateRequest(
            title = "10월 정기 러닝",
            description = "반포 한강공원 집합",
            startAt = startAt,
            endAt = OffsetDateTime.parse("2026-10-11T09:00:00+09:00"),
            location = "반포 한강공원"
        )

        val savedEntity = Schedule(
            id = 20L,
            meetingId = meetingId,
            title = "10월 정기 러닝",
            description = "반포 한강공원 집합",
            startsAt = startAt,
            endedAt = request.endAt,
            location = "반포 한강공원"
        )

        `when`(scheduleRepository.save(any(Schedule::class.java))).thenReturn(savedEntity)

        When("createSchedule 메서드를 호출하면") {
            val response = scheduleService.createSchedule(meetingId, userId, request)

            Then("Schedule 엔티티가 영속화되고 올바른 응답 DTO가 반환된다") {
                response.shouldNotBeNull()
                response.scheduleId shouldBe 20L
                response.title shouldBe "10월 정기 러닝"
                response.startAt shouldBe startAt
                verify(scheduleRepository).save(any(Schedule::class.java))
            }
        }
    }

    Given("모임 일정 목록 조회 기간이 주어졌을 때") {
        val meetingId = 1L
        val userId = 1L
        val from = LocalDate.parse("2026-10-01")
        val to = LocalDate.parse("2026-10-31")
        val rangeStart = OffsetDateTime.parse("2026-10-01T00:00:00+09:00")
        val rangeEnd = OffsetDateTime.parse("2026-11-01T00:00:00+09:00")

        // DB에서 UTC로 읽힌 일시도 응답에서는 KST로 변환되는지 확인
        val schedule = Schedule(
            id = 20L,
            meetingId = meetingId,
            title = "10월 정기 러닝",
            startsAt = OffsetDateTime.parse("2026-10-10T22:00:00Z"),
            endedAt = OffsetDateTime.parse("2026-10-11T00:00:00Z"),
            location = "반포 한강공원"
        )

        `when`(
            scheduleRepository.findAllByMeetingIdAndStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
                meetingId, rangeStart, rangeEnd
            )
        ).thenReturn(listOf(schedule))

        When("getSchedules 메서드를 호출하면") {
            val response = scheduleService.getSchedules(meetingId, userId, from, to)

            Then("to 다음 날 0시 전까지의 일정을 조회하고 일시를 KST로 변환해 반환한다") {
                response.schedules.size shouldBe 1
                val summary = response.schedules[0]
                summary.scheduleId shouldBe 20L
                summary.title shouldBe "10월 정기 러닝"
                summary.startAt.toString() shouldBe "2026-10-11T07:00+09:00"
                summary.endAt.toString() shouldBe "2026-10-11T09:00+09:00"
                summary.location shouldBe "반포 한강공원"
            }
        }

        When("기간 내 일정이 없으면") {
            val response = scheduleService.getSchedules(meetingId, userId, from, from)

            Then("빈 목록을 반환한다") {
                response.schedules.shouldBeEmpty()
            }
        }
    }

    Given("조회 시작일이 종료일보다 이후인 기간이 주어졌을 때") {
        val from = LocalDate.parse("2026-10-31")
        val to = LocalDate.parse("2026-10-01")

        When("getSchedules 메서드를 호출하면") {
            val exception = shouldThrow<ResponseStatusException> {
                scheduleService.getSchedules(1L, 1L, from, to)
            }

            Then("400 Bad Request 예외가 발생하고 일정을 조회하지 않는다") {
                exception.statusCode shouldBe HttpStatus.BAD_REQUEST
                verifyNoInteractions(scheduleRepository)
            }
        }
    }

    Given("모임에 속한 일정 ID가 주어졌을 때") {
        val meetingId = 1L
        val scheduleId = 20L

        // DB에서 UTC로 읽힌 일시도 응답에서는 KST로 변환되는지 확인
        val schedule = Schedule(
            id = scheduleId,
            meetingId = meetingId,
            title = "10월 정기 러닝",
            description = "반포 한강공원 집합",
            startsAt = OffsetDateTime.parse("2026-10-10T22:00:00Z"),
            endedAt = OffsetDateTime.parse("2026-10-11T00:00:00Z"),
            location = "반포 한강공원"
        )

        `when`(scheduleRepository.findByIdAndMeetingId(scheduleId, meetingId)).thenReturn(schedule)

        When("getSchedule 메서드를 호출하면") {
            val response = scheduleService.getSchedule(meetingId, 1L, scheduleId)

            Then("일정 상세를 KST 일시로 반환한다") {
                response.scheduleId shouldBe 20L
                response.title shouldBe "10월 정기 러닝"
                response.description shouldBe "반포 한강공원 집합"
                response.startAt.toString() shouldBe "2026-10-11T07:00+09:00"
                response.endAt.toString() shouldBe "2026-10-11T09:00+09:00"
                response.location shouldBe "반포 한강공원"
            }
        }
    }

    Given("없거나 다른 모임에 속한 일정 ID가 주어졌을 때") {
        When("getSchedule 메서드를 호출하면") {
            val exception = shouldThrow<ResponseStatusException> {
                scheduleService.getSchedule(1L, 1L, 999L)
            }

            Then("404 Not Found 예외가 발생한다") {
                exception.statusCode shouldBe HttpStatus.NOT_FOUND
            }
        }
    }
})

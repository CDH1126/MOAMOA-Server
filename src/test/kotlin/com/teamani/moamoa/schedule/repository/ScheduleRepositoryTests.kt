package com.teamani.moamoa.schedule.repository

import com.teamani.moamoa.schedule.entity.Schedule
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

// 실제 DB(H2)에서 조회 기간과 겹치는 일정만 시작 일시 순으로 조회되는지 검증
@SpringBootTest
@ActiveProfiles("test")
class ScheduleRepositoryTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var scheduleRepository: ScheduleRepository

    init {
        Given("조회 기간(10/11~10/12) 앞뒤와 걸쳐 있는 일정들이 저장되어 있을 때") {
            // 다른 테스트 데이터와 섞이지 않도록 전용 모임 ID 사용
            val meetingId = 9101L

            val multiDay = scheduleRepository.save(schedule(meetingId, "10/9~10/12 MT", "2026-10-09T10:00:00+09:00", "2026-10-12T18:00:00+09:00"))
            val inRange = scheduleRepository.save(schedule(meetingId, "10/11 러닝", "2026-10-11T07:00:00+09:00", "2026-10-11T09:00:00+09:00"))
            val noEndInRange = scheduleRepository.save(schedule(meetingId, "10/12 종료 시간 없음", "2026-10-12T20:00:00+09:00", null))
            scheduleRepository.save(schedule(meetingId, "기간 전에 끝난 일정", "2026-10-01T10:00:00+09:00", "2026-10-02T10:00:00+09:00"))
            scheduleRepository.save(schedule(meetingId, "기간 전 종료 시간 없음", "2026-10-10T23:00:00+09:00", null))
            scheduleRepository.save(schedule(meetingId, "기간 후 일정", "2026-10-13T00:00:00+09:00", null))
            scheduleRepository.save(schedule(9102L, "다른 모임 일정", "2026-10-11T10:00:00+09:00", null))

            When("[10/11 00:00, 10/13 00:00) 범위로 조회하면") {
                val result = scheduleRepository.findAllOverlapping(
                    meetingId,
                    OffsetDateTime.parse("2026-10-11T00:00:00+09:00"),
                    OffsetDateTime.parse("2026-10-13T00:00:00+09:00")
                )

                Then("기간 이전에 시작했지만 겹치는 여러 날 일정까지 포함해 시작 일시 순으로 반환한다") {
                    result.map { it.id } shouldContainExactly listOf(multiDay.id, inRange.id, noEndInRange.id)
                }
            }
        }
    }

    private fun schedule(meetingId: Long, title: String, startsAt: String, endedAt: String?) =
        Schedule(
            meetingId = meetingId,
            title = title,
            startsAt = OffsetDateTime.parse(startsAt),
            endedAt = endedAt?.let { OffsetDateTime.parse(it) }
        )
}

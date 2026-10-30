package com.teamani.moamoa.notice.repository

import com.teamani.moamoa.notice.entity.Notice
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

// 실제 DB(H2)에서 공지 목록의 모임 필터와 최신순 복합 정렬(createdAt desc, id desc)을 검증
@SpringBootTest
@ActiveProfiles("test")
class NoticeRepositoryTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var noticeRepository: NoticeRepository

    init {
        Given("한 모임에 작성 시각이 다르거나 같은 공지와 다른 모임의 공지가 저장되어 있을 때") {
            // 다른 테스트 데이터와 섞이지 않도록 전용 모임 ID 사용
            val meetingId = 9001L
            val earlier = OffsetDateTime.parse("2026-10-01T10:00:00+09:00")
            val later = OffsetDateTime.parse("2026-10-02T10:00:00+09:00")

            val oldest = noticeRepository.save(notice(meetingId, "가장 오래된 공지", earlier))
            val sameTimeFirst = noticeRepository.save(notice(meetingId, "같은 시각 먼저 저장", later))
            val sameTimeSecond = noticeRepository.save(notice(meetingId, "같은 시각 나중 저장", later))
            noticeRepository.save(notice(9002L, "다른 모임 공지", later))

            val sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))

            When("첫 페이지(size=2)를 조회하면") {
                val page = noticeRepository.findAllByMeetingId(meetingId, PageRequest.of(0, 2, sort))

                Then("최신순, 같은 시각이면 나중에 저장된 공지가 먼저 오고 다음 페이지가 있다") {
                    page.content.map { it.id } shouldContainExactly listOf(sameTimeSecond.id, sameTimeFirst.id)
                    page.hasNext() shouldBe true
                }
            }

            When("마지막 페이지를 조회하면") {
                val page = noticeRepository.findAllByMeetingId(meetingId, PageRequest.of(1, 2, sort))

                Then("남은 공지만 반환되고 다음 페이지가 없으며 다른 모임 공지는 포함되지 않는다") {
                    page.content.map { it.id } shouldContainExactly listOf(oldest.id)
                    page.hasNext() shouldBe false
                    page.totalElements shouldBe 3L
                }
            }
        }
    }

    private fun notice(meetingId: Long, title: String, createdAt: OffsetDateTime) =
        Notice(meetingId = meetingId, membersId = 1L, title = title, content = "내용", createdAt = createdAt)
}

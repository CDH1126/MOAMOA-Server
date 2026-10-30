package com.teamani.moamoa.settlement.repository

import com.teamani.moamoa.settlement.entity.Settlement
import com.teamani.moamoa.settlement.entity.SettlementMember
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

// 실제 DB(H2)에서 정산 목록 정렬, 모임 필터, 납부 항목 일괄 조회를 검증
@SpringBootTest
@ActiveProfiles("test")
class SettlementRepositoryTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var settlementRepository: SettlementRepository

    @Autowired
    lateinit var settlementMemberRepository: SettlementMemberRepository

    init {
        Given("한 모임에 생성 시각이 다르거나 같은 정산과 다른 모임의 정산이 저장되어 있을 때") {
            // 다른 테스트 데이터와 섞이지 않도록 전용 모임 ID 사용
            val meetingId = 9201L
            val earlier = "2026-10-01T10:00:00+09:00"
            val later = "2026-10-02T10:00:00+09:00"

            val oldest = settlementRepository.save(settlement(meetingId, earlier))
            val sameTimeFirst = settlementRepository.save(settlement(meetingId, later))
            val sameTimeSecond = settlementRepository.save(settlement(meetingId, later))
            val otherMeeting = settlementRepository.save(settlement(9202L, later))

            val firstId = checkNotNull(sameTimeFirst.id)
            val secondId = checkNotNull(sameTimeSecond.id)
            val payments = settlementMemberRepository.saveAll(
                listOf(
                    SettlementMember(settlementId = firstId, memberId = 1L, amount = 5000),
                    SettlementMember(settlementId = firstId, memberId = 2L, amount = 5000),
                    SettlementMember(settlementId = secondId, memberId = 1L, amount = 10000)
                )
            )

            When("모임의 정산 목록을 조회하면") {
                val result = settlementRepository.findAllByMeetingIdOrderByCreatedAtDescIdDesc(meetingId)

                Then("최신순, 같은 시각이면 나중에 만든 정산이 먼저 오고 다른 모임 정산은 제외된다") {
                    result.map { it.id } shouldContainExactly listOf(sameTimeSecond.id, sameTimeFirst.id, oldest.id)
                }
            }

            When("다른 모임 ID로 정산을 조회하면") {
                val result = settlementRepository.findByIdAndMeetingId(checkNotNull(otherMeeting.id), meetingId)

                Then("조회되지 않는다") {
                    result.shouldBeNull()
                }
            }

            When("여러 정산의 납부 항목을 한 번에 조회하면") {
                val result = settlementMemberRepository.findAllBySettlementIdIn(listOf(firstId, secondId))

                Then("해당 정산들의 납부 항목만 모두 반환한다") {
                    result.map { it.id } shouldContainExactlyInAnyOrder payments.map { it.id }
                }
            }
        }
    }

    private fun settlement(meetingId: Long, createdAt: String) = Settlement(
        meetingId = meetingId,
        title = "정산",
        totalAmount = 10000,
        bankName = "국민은행",
        accountNumber = "123456789012",
        accountHolder = "김모아",
        dueAt = OffsetDateTime.parse("2026-10-20T23:59:00+09:00"),
        createdAt = OffsetDateTime.parse(createdAt)
    )
}

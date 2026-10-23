package com.teamani.moamoa.settlement.repository

import com.teamani.moamoa.settlement.entity.CycleType
import com.teamani.moamoa.settlement.entity.RegularDues
import com.teamani.moamoa.settlement.entity.Settlement
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

// 실제 DB(H2)에서 정기 회비 목록 정렬·모임 필터, 삭제 전 회차 정산 연결 해제를 검증
@SpringBootTest
@ActiveProfiles("test")
class RegularDuesRepositoryTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var regularDuesRepository: RegularDuesRepository

    @Autowired
    lateinit var settlementRepository: SettlementRepository

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    init {
        Given("한 모임의 정기 회비 여러 개와 다른 모임의 정기 회비가 저장되어 있을 때") {
            // 다른 테스트 데이터와 섞이지 않도록 전용 모임 ID 사용
            val meetingId = 9301L
            val first = regularDuesRepository.save(regularDues(meetingId))
            val second = regularDuesRepository.save(regularDues(meetingId))
            val otherMeeting = regularDuesRepository.save(regularDues(9302L))

            When("모임의 정기 회비 목록을 조회하면") {
                val result = regularDuesRepository.findAllByMeetingIdOrderByIdAsc(meetingId)

                Then("등록순으로 반환하고 다른 모임 정기 회비는 제외된다") {
                    result.map { it.id } shouldContainExactly listOf(first.id, second.id)
                    result[0].sendTime shouldBe LocalTime.of(9, 0)
                }
            }

            When("다른 모임 ID로 정기 회비를 조회하면") {
                val result = regularDuesRepository.findByIdAndMeetingId(checkNotNull(otherMeeting.id), meetingId)

                Then("조회되지 않는다") {
                    result.shouldBeNull()
                }
            }
        }

        Given("정기 회비로 생성된 회차 정산이 있을 때") {
            val meetingId = 9303L
            val dues = regularDuesRepository.save(regularDues(meetingId))
            val duesId = checkNotNull(dues.id)
            val billed = settlementRepository.save(settlement(meetingId, duesId, LocalDate.parse("2026-10-01")))
            val manual = settlementRepository.save(settlement(meetingId, null, null))

            When("정산 연결을 해제한 뒤 정기 회비를 삭제하면") {
                transactionTemplate.executeWithoutResult {
                    settlementRepository.clearRegularDuesId(duesId)
                    regularDuesRepository.deleteById(duesId)
                }

                Then("정기 회비는 삭제되고 회차 정산은 연결만 해제된 채 남는다") {
                    regularDuesRepository.findById(duesId).isPresent shouldBe false

                    val remained = settlementRepository.findById(checkNotNull(billed.id)).orElseThrow()
                    remained.regularDuesId.shouldBeNull()
                    remained.billingPeriod shouldBe LocalDate.parse("2026-10-01")
                    settlementRepository.findById(checkNotNull(manual.id)).isPresent shouldBe true
                }
            }
        }
    }

    private fun regularDues(meetingId: Long) = RegularDues(
        meetingId = meetingId,
        createdBy = 1L,
        title = "월 회비",
        amount = 10000,
        cycleType = CycleType.MONTHLY,
        cycleDay = "10",
        dueDays = 7,
        bankName = "국민은행",
        accountNumber = "123456789012",
        accountHolder = "김모아"
    )

    private fun settlement(meetingId: Long, regularDuesId: Long?, billingPeriod: LocalDate?) = Settlement(
        meetingId = meetingId,
        regularDuesId = regularDuesId,
        title = "월 회비",
        totalAmount = 30000,
        bankName = "국민은행",
        accountNumber = "123456789012",
        accountHolder = "김모아",
        dueAt = OffsetDateTime.parse("2026-10-17T23:59:00+09:00"),
        billingPeriod = billingPeriod
    )
}

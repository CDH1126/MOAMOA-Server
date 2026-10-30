package com.teamani.moamoa.meeting.repository

import com.teamani.moamoa.meeting.entity.Meeting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MeetingRepositoryTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var meetingRepository: MeetingRepository

    init {
        Given("진행 중인 모임과 삭제(종료)된 모임이 있을 때") {
            When("삭제되지 않은 모임을 조회하면") {
                Then("삭제된 모임은 id·초대코드·id 목록 조회에서 제외된다") {
                    val active = meetingRepository.save(Meeting(name = "진행 모임", inviteCode = "ACTIVE01"))
                    val ended = meetingRepository.save(Meeting(name = "삭제 모임", inviteCode = "ENDED001").apply { end() })
                    val activeId = active.id.shouldNotBeNull()
                    val endedId = ended.id.shouldNotBeNull()

                    meetingRepository.findByIdAndEndedAtIsNull(activeId).shouldNotBeNull()
                    meetingRepository.findByIdAndEndedAtIsNull(endedId).shouldBeNull()
                    meetingRepository.existsByIdAndEndedAtIsNull(activeId) shouldBe true
                    meetingRepository.existsByIdAndEndedAtIsNull(endedId) shouldBe false
                    meetingRepository.findByInviteCodeAndEndedAtIsNull("ACTIVE01").shouldNotBeNull()
                    meetingRepository.findByInviteCodeAndEndedAtIsNull("ENDED001").shouldBeNull()
                    meetingRepository.findAllByIdInAndEndedAtIsNull(listOf(activeId, endedId))
                        .map { it.id } shouldContainExactly listOf(activeId)
                }
            }
        }

        Given("초대코드로 모임을 조회할 때") {
            When("대소문자가 다른 초대코드로 조회하면") {
                Then("같은 코드로 보지 않는다") {
                    meetingRepository.save(Meeting(name = "대소문자 모임", inviteCode = "CASE0001"))

                    meetingRepository.findByInviteCodeAndEndedAtIsNull("CASE0001").shouldNotBeNull()
                    meetingRepository.findByInviteCodeAndEndedAtIsNull("case0001").shouldBeNull()
                }
            }
        }

        Given("초대코드가 이미 사용 중일 때") {
            When("삭제된 모임의 초대코드로 중복을 확인하면") {
                Then("사용 중인 코드로 판단한다") {
                    meetingRepository.save(Meeting(name = "삭제 모임", inviteCode = "USED0001").apply { end() })

                    meetingRepository.existsByInviteCode("USED0001") shouldBe true
                    meetingRepository.existsByInviteCode("FREE0001") shouldBe false
                }
            }
            When("같은 초대코드로 모임을 저장하면") {
                Then("unique 제약 위반 예외가 발생한다") {
                    meetingRepository.saveAndFlush(Meeting(name = "첫 모임", inviteCode = "DUPLI001"))

                    shouldThrow<DataIntegrityViolationException> {
                        meetingRepository.saveAndFlush(Meeting(name = "둘째 모임", inviteCode = "DUPLI001"))
                    }
                }
            }
        }
    }
}

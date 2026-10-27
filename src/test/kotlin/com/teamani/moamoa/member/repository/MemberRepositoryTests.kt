package com.teamani.moamoa.member.repository

import com.teamani.moamoa.member.entity.Member
import com.teamani.moamoa.member.entity.MemberRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class MemberRepositoryTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var memberRepository: MemberRepository

    init {
        Given("같은 사용자가 같은 모임에 이미 참여했을 때") {
            When("같은 모임·사용자로 구성원을 저장하면") {
                Then("unique 제약 위반 예외가 발생한다") {
                    memberRepository.saveAndFlush(Member(meetingId = 1L, userId = 10L, role = MemberRole.ADMIN))

                    shouldThrow<DataIntegrityViolationException> {
                        memberRepository.saveAndFlush(Member(meetingId = 1L, userId = 10L, role = MemberRole.MEMBER))
                    }
                }
            }
            When("다른 모임에 같은 사용자를 저장하면") {
                Then("정상 저장된다") {
                    memberRepository.saveAndFlush(Member(meetingId = 2L, userId = 20L, role = MemberRole.ADMIN))
                    memberRepository.saveAndFlush(Member(meetingId = 3L, userId = 20L, role = MemberRole.MEMBER))

                    memberRepository.findAllByUserIdOrderByIdAsc(20L).map { it.meetingId } shouldContainExactly listOf(2L, 3L)
                }
            }
        }

        Given("여러 모임에 구성원이 있을 때") {
            When("모임 id 목록으로 구성원 수를 집계하면") {
                Then("요청한 모임별 구성원 수를 한 번에 반환한다") {
                    memberRepository.saveAll(
                        listOf(
                            Member(meetingId = 4L, userId = 40L, role = MemberRole.ADMIN),
                            Member(meetingId = 4L, userId = 41L, role = MemberRole.MEMBER),
                            Member(meetingId = 4L, userId = 42L, role = MemberRole.MEMBER),
                            Member(meetingId = 5L, userId = 40L, role = MemberRole.ADMIN),
                            Member(meetingId = 6L, userId = 43L, role = MemberRole.ADMIN),
                        )
                    )

                    memberRepository.countGroupByMeetingIdIn(listOf(4L, 5L))
                        .associate { it.meetingId to it.memberCount } shouldContainExactly mapOf(4L to 3L, 5L to 1L)
                    memberRepository.countByMeetingId(4L) shouldBe 3L
                }
            }
        }

        Given("역할을 저장할 때") {
            When("구성원을 저장하고 다시 조회하면") {
                Then("ADMIN / MEMBER 값 그대로 조회된다") {
                    memberRepository.saveAndFlush(Member(meetingId = 7L, userId = 70L, role = MemberRole.ADMIN))
                    memberRepository.saveAndFlush(Member(meetingId = 7L, userId = 71L, role = MemberRole.MEMBER))

                    memberRepository.findAllByMeetingIdOrderByIdAsc(7L).map { it.role } shouldContainExactly
                        listOf(MemberRole.ADMIN, MemberRole.MEMBER)
                }
            }
        }
    }
}

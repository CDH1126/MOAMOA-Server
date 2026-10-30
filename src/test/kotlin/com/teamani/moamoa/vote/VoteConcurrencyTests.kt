package com.teamani.moamoa.vote

import com.teamani.moamoa.global.exception.ApiException
import com.teamani.moamoa.global.exception.ErrorCode
import com.teamani.moamoa.meeting.entity.Meeting
import com.teamani.moamoa.meeting.repository.MeetingRepository
import com.teamani.moamoa.member.entity.Member
import com.teamani.moamoa.member.entity.MemberRole
import com.teamani.moamoa.member.repository.MemberRepository
import com.teamani.moamoa.schedule.entity.Schedule
import com.teamani.moamoa.schedule.entity.ScheduleStatus
import com.teamani.moamoa.schedule.repository.ScheduleRepository
import com.teamani.moamoa.vote.dto.MyVoteSubmitRequest
import com.teamani.moamoa.vote.entity.Vote
import com.teamani.moamoa.vote.entity.VoteStatus
import com.teamani.moamoa.vote.repository.VoteRepository
import com.teamani.moamoa.vote.service.VoteService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.reset
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.time.OffsetDateTime
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

// 실제 트랜잭션·DB에서 같은 구성원의 투표 제출이 동시에 들어올 때 응답이 중복 저장되지 않는지 검증
@SpringBootTest
@ActiveProfiles("test")
class VoteConcurrencyTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var voteService: VoteService

    @Autowired
    lateinit var meetingRepository: MeetingRepository

    @Autowired
    lateinit var memberRepository: MemberRepository

    @Autowired
    lateinit var scheduleRepository: ScheduleRepository

    // 두 요청이 모두 기존 응답 삭제를 마친 뒤 저장하도록 실행 순서를 맞추기 위해 spy 사용
    @MockitoSpyBean
    lateinit var voteRepository: VoteRepository

    private val firstTime = OffsetDateTime.parse("2026-11-01T19:00:00+09:00")
    private val secondTime = OffsetDateTime.parse("2026-11-02T19:00:00+09:00")

    init {
        afterTest {
            reset(voteRepository)
            voteRepository.deleteAll()
            scheduleRepository.deleteAll()
            memberRepository.deleteAll()
            meetingRepository.deleteAll()
        }

        Given("후보가 2개인 투표에 아직 응답하지 않은 구성원이 있을 때") {
            When("같은 구성원의 첫 제출 2건이 기존 응답 삭제를 마친 뒤 동시에 저장되면") {
                Then("한 요청만 성공하고 나머지는 409 CONFLICT이며 응답은 후보 수만큼만 저장된다") {
                    val meetingId = checkNotNull(meetingRepository.save(Meeting(name = "모아모아", inviteCode = "RACE0101")).id)
                    val adminMemberId = checkNotNull(
                        memberRepository.save(Member(meetingId = meetingId, userId = 99L, role = MemberRole.ADMIN)).id
                    )
                    val memberId = checkNotNull(
                        memberRepository.save(Member(meetingId = meetingId, userId = USER_ID, role = MemberRole.MEMBER)).id
                    )
                    val scheduleId = checkNotNull(
                        scheduleRepository.save(
                            Schedule(
                                meetingId = meetingId,
                                title = "11월 정기 모임",
                                startsAt = firstTime,
                                status = ScheduleStatus.VOTING
                            )
                        ).id
                    )
                    val deadline = OffsetDateTime.now().plusDays(1)
                    voteRepository.saveAll(
                        listOf(firstTime, secondTime).map {
                            Vote(adminMemberId, scheduleId, it, VoteStatus.PENDING, endedAt = deadline)
                        }
                    )

                    // 첫 제출이라 지울 응답이 없으므로 실제 삭제 대신 두 요청이 모두 삭제 단계에 올 때까지 대기
                    val deleted = CyclicBarrier(2)
                    doAnswer {
                        deleted.await(5, TimeUnit.SECONDS)
                        0
                    }.`when`(voteRepository).deleteAllByScheduleIdAndMemberId(scheduleId, memberId)

                    val request = MyVoteSubmitRequest(listOf(firstTime))
                    val results = runConcurrently(
                        List(2) { { voteService.submitMyVote(meetingId, USER_ID, scheduleId, request) } }
                    )

                    results.count { it == null } shouldBe 1
                    results.filterNotNull().map { (it as ApiException).errorCode } shouldContainExactly listOf(ErrorCode.CONFLICT)
                    voteRepository.findAllByScheduleId(scheduleId).count { it.memberId == memberId } shouldBe 2
                }
            }
        }
    }

    // 모든 작업을 같은 시점에 시작하고, 작업별 예외(성공이면 null)를 순서대로 반환
    private fun runConcurrently(tasks: List<() -> Any?>): List<Throwable?> {
        val ready = CountDownLatch(tasks.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(tasks.size)

        try {
            val futures = tasks.map { task ->
                executor.submit(
                    Callable {
                        ready.countDown()
                        start.await()
                        runCatching { task() }.exceptionOrNull()
                    }
                )
            }
            ready.await()
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    companion object {
        // 투표를 제출하는 구성원의 사용자 ID
        private const val USER_ID = 1L
    }
}

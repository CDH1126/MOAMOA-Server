package com.teamani.moamoa.notice.controller

import com.teamani.moamoa.notice.dto.NoticeListResponse
import com.teamani.moamoa.notice.service.NoticeService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

// 실제 Spring MVC 설정(파라미터 검증 + GlobalExceptionHandler)으로 목록 조회 페이징 파라미터를 검증
@WebMvcTest(controllers = [NoticeController::class])
class NoticeControllerWebMvcTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var noticeService: NoticeService

    init {
        Given("허용 범위를 벗어난 페이징 파라미터로 목록 조회 요청이 주어졌을 때") {
            val invalidParams = mapOf(
                "page=-1" to ("page" to "-1"),
                "size=0" to ("size" to "0"),
                "size=101" to ("size" to "101")
            )

            invalidParams.forEach { (case, param) ->
                When("$case 로 목록 조회 API를 호출하면") {
                    Then("400 Bad Request와 VALIDATION_ERROR 공통 오류 응답을 반환한다") {
                        mockMvc.get("/v1/meetings/1/notices") {
                            param(param.first, param.second)
                        }.andExpect {
                            status { isBadRequest() }
                            jsonPath("$.success") { value(false) }
                            jsonPath("$.error.code") { value("VALIDATION_ERROR") }
                        }
                    }
                }
            }
        }

        Given("페이징 파라미터 없이 목록 조회 요청이 주어졌을 때") {
            When("목록 조회 API를 호출하면") {
                Then("기본값 page=0, size=20으로 조회한다") {
                    `when`(noticeService.getNotices(1L, 0, 20, 1L)).thenReturn(
                        NoticeListResponse(notices = emptyList(), page = 0, size = 20, hasNext = false)
                    )

                    mockMvc.get("/v1/meetings/1/notices").andExpect {
                        status { isOk() }
                        jsonPath("$.data.page") { value(0) }
                        jsonPath("$.data.size") { value(20) }
                    }
                }
            }
        }
    }
}

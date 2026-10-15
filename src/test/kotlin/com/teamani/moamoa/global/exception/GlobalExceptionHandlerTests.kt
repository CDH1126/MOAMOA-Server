package com.teamani.moamoa.global.exception

import com.teamani.moamoa.global.response.CommonResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.not
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@WebMvcTest(controllers = [ExceptionTestController::class, ValidatedTestController::class])
class GlobalExceptionHandlerTests : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    lateinit var mockMvc: MockMvc

    init {
        Given("정상 요청이 주어졌을 때") {
            When("성공 응답을 반환하면") {
                Then("success=true, data, error=null, +09:00 timestamp 형식으로 응답한다") {
                    mockMvc.get("/test/success").andExpect {
                        status { isOk() }
                        jsonPath("$.success") { value(true) }
                        jsonPath("$.data.name") { value("모임") }
                        jsonPath("$.error") { value(null as Any?) }
                        jsonPath("$.timestamp") { value(matchesPattern(TIMESTAMP_PATTERN)) }
                    }
                }
            }
        }

        Given("Service에서 ApiException이 발생했을 때") {
            When("메시지를 지정하지 않으면") {
                Then("ErrorCode의 상태 코드와 기본 메시지로 응답하고 details는 생략한다") {
                    mockMvc.get("/test/not-found").andExpect {
                        expectError(404, "NOT_FOUND")
                        jsonPath("$.error.message") { value("요청한 리소스를 찾을 수 없습니다.") }
                        jsonPath("$.error.details") { doesNotExist() }
                    }
                }
            }
            When("메시지를 지정하면") {
                Then("지정한 메시지로 응답한다") {
                    mockMvc.get("/test/conflict").andExpect {
                        expectError(409, "CONFLICT")
                        jsonPath("$.error.message") { value("이미 가입한 모임입니다.") }
                    }
                }
            }
        }

        Given("요청 본문 검증에 실패했을 때") {
            When("일반 필드가 잘못되면") {
                Then("400 VALIDATION_ERROR와 필드별 details를 응답한다") {
                    mockMvc.post("/test/body") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"","password":"validpassword"}"""
                    }.andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("name") }
                        jsonPath("$.error.details[0].reason") { exists() }
                        jsonPath("$.error.details[0].rejectedValue") { value("") }
                    }
                }
            }
            When("민감 필드(password)가 잘못되면") {
                Then("rejectedValue에 입력값을 노출하지 않는다") {
                    mockMvc.post("/test/body") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"모임","password":"short"}"""
                    }.andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("password") }
                        jsonPath("$.error.details[0].rejectedValue") { value(null as Any?) }
                        content { string(not(containsString("short"))) }
                    }
                }
            }
            When("개인정보 필드(email)가 잘못되면") {
                Then("rejectedValue에 입력값을 노출하지 않는다") {
                    mockMvc.post("/test/body") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"모임","password":"validpassword","email":"user-at-mail"}"""
                    }.andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("email") }
                        jsonPath("$.error.details[0].rejectedValue") { value(null as Any?) }
                        content { string(not(containsString("user-at-mail"))) }
                    }
                }
            }
            When("경로 변수 제약과 함께 요청 본문 검증에 실패하면") {
                Then("본문 전체가 아닌 필드 단위로 응답하고 민감 필드 입력값을 노출하지 않는다") {
                    mockMvc.post("/test/body/1") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"모임","password":"short"}"""
                    }.andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("password") }
                        jsonPath("$.error.details[0].rejectedValue") { value(null as Any?) }
                        content { string(not(containsString("short"))) }
                    }
                }
            }
        }

        Given("요청 파라미터 검증에 실패했을 때") {
            When("@Validated 없는 Controller의 파라미터가 잘못되면") {
                Then("400 VALIDATION_ERROR와 파라미터 이름을 field로 응답한다") {
                    mockMvc.get("/test/params?page=-1").andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("page") }
                        jsonPath("$.error.details[0].rejectedValue") { value(-1) }
                    }
                }
            }
            When("@Validated Controller의 파라미터가 잘못되면") {
                Then("400 VALIDATION_ERROR와 파라미터 이름을 field로 응답한다") {
                    mockMvc.get("/test/validated?page=-1").andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("page") }
                    }
                }
            }
            When("민감 파라미터(token)가 잘못되면") {
                Then("rejectedValue에 입력값을 노출하지 않는다") {
                    mockMvc.get("/test/token?token=abc").andExpect {
                        expectError(400, "VALIDATION_ERROR")
                        jsonPath("$.error.details[0].field") { value("token") }
                        jsonPath("$.error.details[0].rejectedValue") { value(null as Any?) }
                    }
                }
            }
        }

        Given("요청 형식이 잘못되었을 때") {
            When("JSON 본문을 해석할 수 없으면") {
                Then("400 JSON_PARSE_ERROR로 응답한다") {
                    mockMvc.post("/test/body") {
                        contentType = MediaType.APPLICATION_JSON
                        content = """{"name":"""
                    }.andExpect { expectError(400, "JSON_PARSE_ERROR") }
                }
            }
            When("파라미터 타입이 맞지 않으면") {
                Then("400 INPUT_ERROR로 응답한다") {
                    mockMvc.get("/test/params?page=abc").andExpect { expectError(400, "INPUT_ERROR") }
                }
            }
            When("필수 파라미터가 없으면") {
                Then("400 MISSING_REQUIRED_VALUE로 응답한다") {
                    mockMvc.get("/test/required").andExpect { expectError(400, "MISSING_REQUIRED_VALUE") }
                }
            }
            When("필수 헤더가 없으면") {
                Then("400 MISSING_REQUIRED_VALUE로 응답한다") {
                    mockMvc.get("/test/header").andExpect { expectError(400, "MISSING_REQUIRED_VALUE") }
                }
            }
        }

        Given("지원하지 않는 요청이 주어졌을 때") {
            When("존재하지 않는 URL을 요청하면") {
                Then("404 NOT_FOUND로 응답한다") {
                    mockMvc.get("/test/unknown-path").andExpect { expectError(404, "NOT_FOUND") }
                }
            }
            When("허용되지 않은 HTTP 메서드로 요청하면") {
                Then("405 METHOD_NOT_ALLOWED와 Allow 헤더로 응답한다") {
                    mockMvc.put("/test/success").andExpect {
                        expectError(405, "METHOD_NOT_ALLOWED")
                        header { string("Allow", containsString("GET")) }
                    }
                }
            }
            When("지원하지 않는 Content-Type으로 요청하면") {
                Then("415 UNSUPPORTED_MEDIA_TYPE으로 응답한다") {
                    mockMvc.post("/test/body") {
                        contentType = MediaType.TEXT_PLAIN
                        content = "name"
                    }.andExpect { expectError(415, "UNSUPPORTED_MEDIA_TYPE") }
                }
            }
        }

        Given("예상하지 못한 예외가 발생했을 때") {
            When("요청을 처리하면") {
                Then("500 INTERNAL_ERROR로 응답하고 내부 메시지를 노출하지 않는다") {
                    mockMvc.get("/test/unexpected").andExpect {
                        expectError(500, "INTERNAL_ERROR")
                        jsonPath("$.error.message") { value("서버 내부 오류가 발생했습니다.") }
                        content { string(not(containsString("internal-detail"))) }
                    }
                }
            }
        }
    }

    // 실패 응답 공통 형식 검증 (가이드 §8 검증 체크리스트)
    private fun MockMvcResultMatchersDsl.expectError(httpStatus: Int, code: String) {
        status { isEqualTo(httpStatus) }
        jsonPath("$.success") { value(false) }
        jsonPath("$.data") { value(null as Any?) }
        jsonPath("$.error.code") { value(code) }
        jsonPath("$.error.message") { exists() }
        jsonPath("$.timestamp") { value(matchesPattern(TIMESTAMP_PATTERN)) }
    }

    companion object {
        private const val TIMESTAMP_PATTERN = """\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\+09:00"""
    }
}

data class TestBodyRequest(
    @field:NotBlank
    val name: String?,
    @field:Size(min = 8)
    val password: String?,
    @field:Email
    val email: String? = null,
)

data class TestData(val name: String)

@RestController
class ExceptionTestController {

    @GetMapping("/test/success")
    fun success() = CommonResponse(data = TestData("모임"))

    @GetMapping("/test/not-found")
    fun notFound(): CommonResponse<TestData> = throw ApiException(ErrorCode.NOT_FOUND)

    @GetMapping("/test/conflict")
    fun conflict(): CommonResponse<TestData> = throw ApiException(ErrorCode.CONFLICT, "이미 가입한 모임입니다.")

    @PostMapping("/test/body")
    fun body(@Valid @RequestBody request: TestBodyRequest) = CommonResponse(data = TestData(request.name.orEmpty()))

    @PostMapping("/test/body/{id}")
    fun bodyWithPath(@PathVariable @Positive id: Long, @Valid @RequestBody request: TestBodyRequest) =
        CommonResponse(data = TestData(request.name.orEmpty()))

    @GetMapping("/test/params")
    fun params(@RequestParam(defaultValue = "0") @Min(0) page: Int) = CommonResponse(data = TestData("$page"))

    @GetMapping("/test/token")
    fun token(@RequestParam @Size(min = 10) token: String) = CommonResponse(data = TestData(token))

    @GetMapping("/test/required")
    fun required(@RequestParam from: String) = CommonResponse(data = TestData(from))

    @GetMapping("/test/header")
    fun header(@RequestHeader("X-User-Id") userId: Long) = CommonResponse(data = TestData("$userId"))

    @GetMapping("/test/unexpected")
    fun unexpected(): CommonResponse<TestData> = throw IllegalStateException("internal-detail")
}

@Validated
@RestController
class ValidatedTestController {

    @GetMapping("/test/validated")
    fun validated(@RequestParam(defaultValue = "0") @Min(0) page: Int) = CommonResponse(data = TestData("$page"))
}

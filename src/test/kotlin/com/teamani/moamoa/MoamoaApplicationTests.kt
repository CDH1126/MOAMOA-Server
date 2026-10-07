package com.teamani.moamoa

import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.extensions.spring.testContextManager
import io.kotest.matchers.nulls.shouldNotBeNull
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class MoamoaApplicationTests : FunSpec({
    extensions(SpringExtension())

    test("contextLoads") {
        testContextManager().testContext.applicationContext.shouldNotBeNull()
    }
})

package com.teamani.moamoa.global.response

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

// 응답 timestamp 생성 (ISO-8601, +09:00, 초 단위)
internal fun currentTimestamp(): OffsetDateTime = OffsetDateTime.now(SEOUL_ZONE).truncatedTo(ChronoUnit.SECONDS)

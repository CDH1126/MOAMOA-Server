package com.teamani.moamoa.meeting.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Entity
@Table(name = "meeting")
class Meeting(
    @Column(nullable = false, length = 20)
    var name: String,

    // 모임 소개 글 (선택 값이라 NULL 허용)
    @Column(nullable = true, columnDefinition = "text")
    var description: String? = null,

    // 모임 초대코드 (모임별 고유값)
    @Column(nullable = false, unique = true, length = 20)
    val inviteCode: String,

    @Column(nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    // 모임 종료(삭제) 일시 (NULL이면 진행 중인 모임)
    @Column(nullable = true)
    var endedAt: OffsetDateTime? = null,

    // 기본 키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
) {

    // 전달된 값만 변경 (null이면 기존 값 유지, description은 Optional.empty면 비움)
    fun update(name: String?, description: Optional<String>?) {
        name?.let { this.name = it }
        description?.let { this.description = it.getOrNull() }
    }

    // 종료 일시 기록으로 모임 삭제 처리 (soft delete)
    fun end() {
        endedAt = OffsetDateTime.now()
    }
}

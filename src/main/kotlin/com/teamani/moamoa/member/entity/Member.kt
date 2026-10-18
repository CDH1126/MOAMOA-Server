package com.teamani.moamoa.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "member",
    uniqueConstraints = [
        // 같은 사용자의 같은 모임 중복 참여 방지
        UniqueConstraint(name = "uk_member_meeting_id_user_id", columnNames = ["meeting_id", "user_id"]),
    ],
)
class Member(
    // 구성원이 속한 모임 ID
    @Column(nullable = false)
    val meetingId: Long,

    // 구성원 사용자 ID
    @Column(nullable = false)
    val userId: Long,

    // 모임 내 권한 (ADMIN / MEMBER)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: MemberRole,

    // 기본 키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null
)

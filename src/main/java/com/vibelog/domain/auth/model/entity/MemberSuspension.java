package com.vibelog.domain.auth.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [MemberSuspension]
 * 역할 : 제재 기록 테이블(member_suspension)과 매핑된다. 운영자가 사유를 넣어 남긴다 (FR-USR-010, PL-RPT-005).
 * 책임 : - 컬럼은 sql/01-schema.sql 의 member_suspension 그대로 둔다. 7일·30일만 끝나는 시각(ends_at)이 있다 (ck_member_suspension_period).
 *        - 기간이 끝나면 따로 처리하지 않아도 정지가 아니다 (자동 해제, 배치 없음). 운영자가 풀면 released_at 을 남긴다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSuspension extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberSuspensionId;

    private Long memberId;

    private Long adminMemberId;

    @Enumerated(EnumType.STRING)
    private SuspensionLevel level;

    private String reason;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private LocalDateTime releasedAt;

    private Long releaserMemberId;

    public static MemberSuspension create(Long memberId, Long adminMemberId, SuspensionLevel level, String reason,
                                          LocalDateTime now) {
        MemberSuspension suspension = new MemberSuspension();
        suspension.memberId = memberId;
        suspension.adminMemberId = adminMemberId;
        suspension.level = level;
        suspension.reason = reason;
        suspension.startsAt = now;
        suspension.endsAt = level.getDays() > 0 ? now.plusDays(level.getDays()) : null;
        return suspension;
    }

    /* 지금 로그인·작성을 막는 제재인가. 경고는 막지 않는다 */
    public boolean isActiveAt(LocalDateTime now) {
        return releasedAt == null && level.blocks() && (endsAt == null || endsAt.isAfter(now));
    }

    public void release(Long releaserMemberId, LocalDateTime now) {
        this.releasedAt = now;
        this.releaserMemberId = releaserMemberId;
    }
}

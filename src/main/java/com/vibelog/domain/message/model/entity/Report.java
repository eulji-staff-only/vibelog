package com.vibelog.domain.message.model.entity;

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
 * [Report]
 * 역할 : 신고 테이블(report)과 매핑된다. 회원이 콘텐츠 하나를 신고한 한 건 (FR-RPT-001, P3).
 * 책임 : - 컬럼은 sql/01-schema.sql 의 report 그대로 둔다. 같은 대상은 1인 1회 (uk_report_reporter_target).
 *        - target_member_id 는 신고할 때의 대상 작성자다 (#270). 작성자 이력·신고 이력을 report 만으로 구한다.
 *        - 숨김 여부는 컬럼 없이 이 테이블에서 계산한다 (MessageReader, message 명세 "숨김 여부").
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    private Long reporterMemberId;

    @Enumerated(EnumType.STRING)
    private ReportTargetType targetType;

    private Long targetId;

    private Long targetMemberId;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    private String reasonDetail;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private Long handlerMemberId;

    private LocalDateTime handledAt;

    private Report(Long reporterMemberId, ReportTargetType targetType, Long targetId, Long targetMemberId,
                   ReportReason reason, String reasonDetail) {
        this.reporterMemberId = reporterMemberId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetMemberId = targetMemberId;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
        this.status = ReportStatus.PENDING;
    }

    /* 신고 접수 (API-RPT-01). 처리 대기로 시작한다 */
    public static Report create(Long reporterMemberId, ReportTargetType targetType, Long targetId, Long targetMemberId,
                                ReportReason reason, String reasonDetail) {
        return new Report(reporterMemberId, targetType, targetId, targetMemberId, reason, reasonDetail);
    }

    public boolean isPending() {
        return status == ReportStatus.PENDING;
    }

    /* 운영자가 숨김으로 확정한다 (API-RPT-03, PL-RPT-004) */
    public void hide(Long handlerMemberId) {
        handle(ReportStatus.HIDDEN, handlerMemberId);
    }

    /* 운영자가 기각한다 (API-RPT-04). 기각하면 다시 보이고, 그 뒤 신고는 0건부터 센다 */
    public void reject(Long handlerMemberId) {
        handle(ReportStatus.REJECTED, handlerMemberId);
    }

    private void handle(ReportStatus status, Long handlerMemberId) {
        this.status = status;
        this.handlerMemberId = handlerMemberId;
        this.handledAt = LocalDateTime.now();
    }

    /* 운영자가 숨김으로 확정했고, 그 시각이 at 이전인가 (콘테스트 시상 기준 시각, PL-CTS-006) */
    public boolean isHiddenBy(LocalDateTime at) {
        return status == ReportStatus.HIDDEN && handledAt != null && !handledAt.isAfter(at);
    }
}

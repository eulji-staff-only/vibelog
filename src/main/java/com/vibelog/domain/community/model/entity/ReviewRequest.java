package com.vibelog.domain.community.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [ReviewRequest]
 * 역할 : 공개 포트폴리오의 동료 리뷰 요청 (review_request, FR-REV-001).
 * 책임 : 열린 요청 = closed_at 이 없고 요청 후 14일이 지나지 않음 (PL-REV-002). 14일은 볼 때 계산하고 배치로 닫지 않는다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewRequest extends BaseTimeEntity {

    public static final int OPEN_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewRequestId;

    private Long portfolioId;

    private LocalDateTime requestedAt;

    private LocalDateTime closedAt;

    private ReviewRequest(Long portfolioId, LocalDateTime requestedAt) {
        this.portfolioId = portfolioId;
        this.requestedAt = requestedAt;
    }

    public static ReviewRequest create(Long portfolioId) {
        return new ReviewRequest(portfolioId, LocalDateTime.now());
    }

    public boolean isOpen(LocalDateTime now) {
        return closedAt == null && requestedAt.isAfter(now.minusDays(OPEN_DAYS));
    }

    /* API-REV-07 마감, 계약 ⑭ 포트폴리오 비공개·삭제 */
    public void close() {
        if (closedAt == null) {
            closedAt = LocalDateTime.now();
        }
    }
}

package com.vibelog.domain.lesson.model.policy;

/* 포트폴리오 접근 권한 (계약 ⑥). unlocked = enrollment.portfolio_unlocked_at 이 있음 */
public record PortfolioAccess(boolean enrolled, boolean unlocked) {
}

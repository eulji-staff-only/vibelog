package com.vibelog.domain.lesson.model.dto;

/*
 * [PortfolioStatus]
 * 역할 : 내 학습(U-12, API-ENR-02) 코스의 포트폴리오 상태.
 * 책임 : enrollment.portfolio_unlocked_at 이 없으면 잠김, 있고 계약 ⑧(LessonPortfolioChecker.hasSubmitted)이
 *        false 면 제출 전, true 면 제출 완료다.
 */
public enum PortfolioStatus {

    LOCKED("잠김", "ml-badge-locked"),
    OPEN("제출 전", "ml-badge-strong"),
    SUBMITTED("제출 완료", "ml-badge-success");

    private final String label;
    private final String badgeClass;

    PortfolioStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String label() {
        return label;
    }

    public String badgeClass() {
        return badgeClass;
    }
}

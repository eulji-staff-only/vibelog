package com.vibelog.domain.lesson.model.dto;

/*
 * [LessonStatus]
 * 역할 : 내 학습(U-12, API-ENR-02) 수업별 상태.
 * 책임 : lesson_progress 가 없거나 unlocked_at 이 없으면 잠김, completed_at 이 있으면 완료,
 *        그 외(unlocked_at 은 있고 completed_at 은 없음)는 진행 중이다.
 */
public enum LessonStatus {

    LOCKED("잠김", "ml-badge-locked"),
    IN_PROGRESS("진행 중", "ml-badge-strong"),
    COMPLETED("완료", "ml-badge-success");

    private final String label;
    private final String badgeClass;

    LessonStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String label() {
        return label;
    }

    public String badgeClass() {
        return badgeClass;
    }

    public boolean isLocked() {
        return this == LOCKED;
    }
}

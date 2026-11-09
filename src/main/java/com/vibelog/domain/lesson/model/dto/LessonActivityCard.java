package com.vibelog.domain.lesson.model.dto;

import com.vibelog.domain.lesson.model.entity.BodyFormat;

import java.math.BigDecimal;
import java.util.List;

/*
 * 수업 화면(U-13) 왼쪽 활동 목록 한 칸 + (선택됐으면) 오른쪽 내용을 함께 담는 화면 전용 값.
 * kind 에 따라 쓰는 필드가 다르다 (API-LSN-01):
 *  - CONCEPT: title, status, body, bodyFormat, conceptTags, toolNoteUrl, quizActivityId(완료 후 "퀴즈 다시 보기"),
 *             conceptTagIds(conceptTags 와 같은 순서, 태그 칩 → 검색 API-LSN-09), bookmarked(API-LSN-08),
 *             relatedConcepts(연관 개념, API-LSN-07)
 *  - QUIZ: title, status, bestCorrectRate(계약 ⑤, 없으면 null)
 *  - EXAM: title, status(LOCKED·IN_PROGRESS 만 씀), examUrl(열렸을 때), reportUrl(제출한 회차가 있을 때 "지난 리포트 다시 보기", FR-EXM-006)
 *  - status == LOCKED 면 kind 상관없이 previousActivityId(앞 활동 바로가기, FR-LSN-009)만 쓴다
 */
public record LessonActivityCard(
        Long activityId,
        String kind,
        String title,
        ActivityStatus status,
        boolean selected,
        String body,
        BodyFormat bodyFormat,
        List<String> conceptTags,
        String toolNoteUrl,
        Long quizActivityId,
        BigDecimal bestCorrectRate,
        String examUrl,
        Long previousActivityId,
        List<Long> conceptTagIds,
        boolean bookmarked,
        List<RelatedConcept> relatedConcepts,
        String reportUrl
) {
    public static final String CONCEPT = "CONCEPT";
    public static final String QUIZ = "QUIZ";
    public static final String EXAM = "EXAM";

    /* 개념 태그·북마크·연관 개념이 필요 없는 카드(퀴즈·모의고사·잠김)용 */
    public LessonActivityCard(Long activityId, String kind, String title, ActivityStatus status, boolean selected,
                              String body, BodyFormat bodyFormat, List<String> conceptTags, String toolNoteUrl,
                              Long quizActivityId, BigDecimal bestCorrectRate, String examUrl, Long previousActivityId) {
        this(activityId, kind, title, status, selected, body, bodyFormat, conceptTags, toolNoteUrl, quizActivityId,
                bestCorrectRate, examUrl, previousActivityId, List.of(), false, List.of(), null);
    }

    /* 지난 리포트 링크가 필요 없는 카드(개념·퀴즈·미리보기)용 */
    public LessonActivityCard(Long activityId, String kind, String title, ActivityStatus status, boolean selected,
                              String body, BodyFormat bodyFormat, List<String> conceptTags, String toolNoteUrl,
                              Long quizActivityId, BigDecimal bestCorrectRate, String examUrl, Long previousActivityId,
                              List<Long> conceptTagIds, boolean bookmarked, List<RelatedConcept> relatedConcepts) {
        this(activityId, kind, title, status, selected, body, bodyFormat, conceptTags, toolNoteUrl, quizActivityId,
                bestCorrectRate, examUrl, previousActivityId, conceptTagIds, bookmarked, relatedConcepts, null);
    }

    public LessonActivityCard select() {
        return new LessonActivityCard(activityId, kind, title, status, true, body, bodyFormat, conceptTags,
                toolNoteUrl, quizActivityId, bestCorrectRate, examUrl, previousActivityId,
                conceptTagIds, bookmarked, relatedConcepts, reportUrl);
    }

    /* 템플릿에서 복잡한 삼항식을 쓰지 않도록 화면 표시용 값을 미리 계산해 둔다 */
    public boolean isLocked() {
        return status == ActivityStatus.LOCKED;
    }

    public boolean isCompleted() {
        return status == ActivityStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == ActivityStatus.IN_PROGRESS;
    }

    public boolean isConcept() {
        return CONCEPT.equals(kind);
    }

    public boolean isQuiz() {
        return QUIZ.equals(kind);
    }

    public boolean isExam() {
        return EXAM.equals(kind);
    }

    public String kindLabel() {
        if (isConcept()) {
            return "개념";
        }
        return isQuiz() ? "퀴즈" : "모의고사";
    }

    public String statusLabel() {
        if (isCompleted()) {
            return "완료";
        }
        return isInProgress() ? "진행 중" : "잠김";
    }

    public String statusBadgeClass() {
        if (isCompleted()) {
            return "ls-badge-success";
        }
        return isInProgress() ? "ls-badge-strong" : "ls-badge-locked";
    }
}

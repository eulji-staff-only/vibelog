package com.vibelog.domain.lesson.model.policy;

/*
 * [ExamLessonProgress]
 * 역할 : exam 영역이 모의고사가 열렸는지 묻고, 리포트 확인을 기록할 때 쓰는 계약 (계약 ②③).
 * 책임 : lesson 영역(domain/lesson)이 구현한다. docs/design/api/lesson.md "영역 간 계약" 표 그대로.
 */
public interface ExamLessonProgress {

    boolean isExamOpen(Long memberId, Long lessonId);

    ReportNextStep recordReportViewed(Long memberId, Long lessonId);
}

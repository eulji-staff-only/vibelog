package com.vibelog.domain.lesson.model.policy;

/* 퀴즈 활동 정보 (계약 ①). unlocked = 수강 중이고 그 활동이 열려 있음, passRate = 통과 기준 정답률 */
public record QuizActivityInfo(Long courseId, Long lessonId, boolean unlocked, Integer passRate) {
}

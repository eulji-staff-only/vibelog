package com.vibelog.domain.lesson.model.dto;

/* 퀴즈 활동이 속한 코스·수업 (LessonReader.getQuizActivityPlaces, 틀린 문제 모아보기 quiz #190) */
public record QuizActivityPlace(Long courseId, Long lessonId) {
}

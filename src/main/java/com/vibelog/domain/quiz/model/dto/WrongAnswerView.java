package com.vibelog.domain.quiz.model.dto;

import java.util.List;

/*
 * 틀린 문제 모아보기 화면(U-28, API-LSN-10, FR-LSN-008, #190) 데이터.
 * 필터 칸은 내가 틀린 문항이 있는 코스·수업만. 문항은 최근에 틀린 순으로 한 쪽 20개 (page 는 1부터).
 */
public record WrongAnswerView(List<Item> items, int page, int totalPages, long totalCount,
                              List<Option> courseOptions, List<Option> lessonOptions, Long selectedCourseId, Long selectedLessonId) {

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isFiltered() {
        return selectedCourseId != null || selectedLessonId != null;
    }

    /* 필터 칸의 코스·수업 하나 (수업은 courseId 로 어느 코스 것인지 안다) */
    public record Option(Long id, String title, Long courseId) {
    }

    /* 틀린 문항 하나. conceptUrl 은 그 문항의 개념 태그가 붙은 개념 화면 (없으면 수업 화면) */
    public record Item(Long quizQuestionId, String courseTitle, String lessonTitle, String displayTypeLabel, String content,
                       String codeSnippet, String myAnswer, String correctAnswer, String explanation, String conceptTagName,
                       String conceptUrl) {
    }
}

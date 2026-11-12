package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 코스 카드 한 장 (API-CRS-01·02): 코스명, 한 줄 소개, 트랙·카테고리 이름, 플랜 일수, 공개 수업 수.
 * matchedLessonTitles: 코스 목록 검색(API-CRS-02 P2, #189)에서 수업 제목으로 걸렸을 때 그 수업 제목들. 아니면 빈 목록
 */
public record CourseCard(Long courseId, Long trackId, String title, String summary, String trackName,
                         String categoryName, int planDays, int lessonCount, List<String> matchedLessonTitles) {

    /* 검색이 없는 화면(트랙 소개·진단 결과 등)과 기존 호출용. 걸린 수업 없음 */
    public CourseCard(Long courseId, Long trackId, String title, String summary, String trackName,
                      String categoryName, int planDays, int lessonCount) {
        this(courseId, trackId, title, summary, trackName, categoryName, planDays, lessonCount, List.of());
    }
}

package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 코스 목록(U-10, API-CRS-02) 화면 데이터. recommended 는 진단을 마친 회원만 (없으면 null).
 * P2(#189): 카테고리 칩(categories)·선택한 카테고리·검색어(q, 앞뒤 공백 제거, 없으면 null)
 */
public record CourseListView(List<TrackChip> tracks, Long selectedTrackId, TrackCourseGroup recommended,
                             List<TrackCourseGroup> groups, int totalCount,
                             List<CategoryChip> categories, Long selectedCategoryId, String q) {

    /* 카테고리·검색이 없는 기존 호출용 */
    public CourseListView(List<TrackChip> tracks, Long selectedTrackId, TrackCourseGroup recommended,
                          List<TrackCourseGroup> groups, int totalCount) {
        this(tracks, selectedTrackId, recommended, groups, totalCount, List.of(), null, null);
    }

    public boolean isEmpty() {
        return totalCount == 0;
    }

    /* 트랙·카테고리·검색 중 하나라도 걸려 있는가 ("조건 초기화" 표시) */
    public boolean isFiltered() {
        return selectedTrackId != null || selectedCategoryId != null || q != null;
    }
}

package com.vibelog.domain.course.model.dto;

import java.util.List;

/*
 * 코스 목록 화면(U-10, API-CRS-02) 데이터.
 * - recommended: 진단을 마친 회원(COMPLETED)의 추천 트랙과 그 코스. 비회원·진단 건너뛴 회원은 null.
 * - groups: 트랙별로 묶은 공개 코스 (track 필터가 있으면 그 트랙만). 공개 코스가 없는 트랙은 넣지 않는다.
 * - totalCount: groups 안 코스 수 (결과 수). 0이면 화면이 "조건에 맞는 코스가 없습니다" 안내 상자를 보여준다.
 */
public record CourseListView(List<TrackChip> tracks, Long selectedTrackId, TrackCourseGroup recommended,
                             List<TrackCourseGroup> groups, int totalCount) {

    public boolean isEmpty() {
        return totalCount == 0;
    }
}

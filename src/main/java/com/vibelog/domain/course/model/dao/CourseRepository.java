package com.vibelog.domain.course.model.dao;

import com.vibelog.domain.course.model.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    /* 공개 코스 전체, 만든 순서 (코스 목록, API-CRS-02) */
    List<Course> findByIsPublicTrueOrderByCourseIdAsc();

    /* 한 트랙의 공개 코스, 만든 순서 (트랙 소개 API-CRS-01, 추천 트랙·트랙 필터 API-CRS-02) */
    List<Course> findByTrackIdAndIsPublicTrueOrderByCourseIdAsc(Long trackId);

    /* 트랙·카테고리별 코스 수 (공개·비공개 모두). 관리 목록의 코스 수와 삭제 막기(TRACK_HAS_COURSES 등)에 쓴다 */
    long countByTrackId(Long trackId);

    long countByCourseCategoryId(Long courseCategoryId);
}

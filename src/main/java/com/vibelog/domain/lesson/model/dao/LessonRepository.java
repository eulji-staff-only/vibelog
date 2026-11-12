package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    boolean existsByCourseIdAndIsPublicTrue(Long courseId);

    List<Lesson> findByCourseIdAndIsPublicTrueOrderBySortOrderAsc(Long courseId);

    /* 운영자 수업 관리(API-ADM-18·19, #100): 공개·비공개 모두 포함한 그 코스의 전체 수업 */
    List<Lesson> findByCourseIdOrderBySortOrderAsc(Long courseId);

    /* 새 수업의 다음 순서 계산(API-ADM-17, #100) */
    Optional<Lesson> findTopByCourseIdOrderBySortOrderDesc(Long courseId);

    Optional<Lesson> findFirstByCourseIdAndIsPublicTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
            Long courseId, int sortOrder);

    @Query("select new com.vibelog.domain.lesson.model.dao.CourseLessonCount(l.courseId, count(l)) "
            + "from Lesson l where l.courseId in :courseIds and l.isPublic = true group by l.courseId")
    List<CourseLessonCount> countPublicLessonsByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    /* 제목에 검색어가 들어 있는 공개 수업 (부분 일치, 코스 목록 검색 API-CRS-02 P2, course #189) */
    List<Lesson> findByIsPublicTrueAndTitleContainingOrderByCourseIdAscSortOrderAsc(String title);
}

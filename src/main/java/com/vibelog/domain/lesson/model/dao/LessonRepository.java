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

    Optional<Lesson> findFirstByCourseIdAndIsPublicTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
            Long courseId, int sortOrder);

    @Query("select new com.vibelog.domain.lesson.model.dao.CourseLessonCount(l.courseId, count(l)) "
            + "from Lesson l where l.courseId in :courseIds and l.isPublic = true group by l.courseId")
    List<CourseLessonCount> countPublicLessonsByCourseIds(@Param("courseIds") Collection<Long> courseIds);
}

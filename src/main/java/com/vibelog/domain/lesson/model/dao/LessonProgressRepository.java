package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    long countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(Long enrollmentId, Collection<Long> lessonIds);
}

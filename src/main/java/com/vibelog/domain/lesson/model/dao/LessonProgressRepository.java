package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    long countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(Long enrollmentId, Collection<Long> lessonIds);

    /* 내 학습(API-ENR-02) 수업별 상태 계산용 — 한 번의 쿼리로 가져온다 (N+1 방지) */
    List<LessonProgress> findByEnrollmentIdAndLessonIdIn(Long enrollmentId, Collection<Long> lessonIds);
}

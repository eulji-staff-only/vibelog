package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByMemberIdAndCourseId(Long memberId, Long courseId);

    boolean existsByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(Long memberId);

    /* 회원의 수강 전체, 시작한 순서 (LessonReader.getEnrollmentsOf) */
    List<Enrollment> findByMemberIdOrderByStartedAtAsc(Long memberId);
}

package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByMemberIdAndCourseId(Long memberId, Long courseId);

    boolean existsByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(Long memberId);

    /* 내 학습(API-ENR-02) "수강 중" 탭: 완료·포기하지 않은 수강 */
    List<Enrollment> findByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(Long memberId);

    /* 내 학습(API-ENR-02) "완료" 탭 */
    List<Enrollment> findByMemberIdAndCompletedAtIsNotNull(Long memberId);

    /* 회원의 수강 전체, 시작한 순서 (LessonReader.getEnrollmentsOf) */
    List<Enrollment> findByMemberIdOrderByStartedAtAsc(Long memberId);
}

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

    /* 수업 공개(API-ADM-33, PL-ENR-005, #118): 새로 공개된 수업의 진도를 기존 수강에 추가할 때 쓴다 */
    List<Enrollment> findByCourseId(Long courseId);

    /* 수업 비공개 영향 확인(API-ADM-32, FR-LSN-014, #118) */
    long countByCourseIdAndCompletedAtIsNullAndDroppedAtIsNull(Long courseId);
}

package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByMemberIdAndCourseId(Long memberId, Long courseId);

    /*
     * 포기하지 않은 수강만 (진행 기록을 바꾸는 경로가 쓴다, #306). 포기한 수강은 "수강 안 함"과 같게 없는 것으로 본다.
     * 포기 여부만 보므로 완료한 코스(복습, PL-ENR-004)는 그대로 찾힌다
     */
    Optional<Enrollment> findByMemberIdAndCourseIdAndDroppedAtIsNull(Long memberId, Long courseId);

    boolean existsByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(Long memberId);

    /* 내 학습(API-ENR-02) "수강 중" 탭: 완료·포기하지 않은 수강 */
    List<Enrollment> findByMemberIdAndCompletedAtIsNullAndDroppedAtIsNull(Long memberId);

    /* 내 학습(API-ENR-02) "완료" 탭 */
    List<Enrollment> findByMemberIdAndCompletedAtIsNotNullAndDroppedAtIsNull(Long memberId);

    /* 회원의 수강 전체, 시작한 순서 (LessonReader.getEnrollmentsOf) */
    List<Enrollment> findByMemberIdOrderByStartedAtAsc(Long memberId);

    /* 여러 회원의 수강 전체 (LessonReader.findEnrolledCourseIds, admin 확장 대시보드 추천 트랙 vs 실제 수강, FR-ADM-003) */
    List<Enrollment> findByMemberIdIn(java.util.Collection<Long> memberIds);

    /* 수업 공개(API-ADM-33, PL-ENR-005, #118): 새로 공개된 수업의 진도를 기존 수강에 추가할 때 쓴다 */
    List<Enrollment> findByCourseId(Long courseId);

    /*
     * 기간 내 수강 시작한 수강 전체 (started_at 기준). from·to 가 null 이면 그 쪽 제한 없음
     * (운영자 대시보드 API-ADM-01, FR-ADM-002, LessonReader.getEnrollmentStatsByCourse)
     */
    @Query("select e from Enrollment e where (:from is null or e.startedAt >= :from) and (:to is null or e.startedAt <= :to)")
    List<Enrollment> findStartedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

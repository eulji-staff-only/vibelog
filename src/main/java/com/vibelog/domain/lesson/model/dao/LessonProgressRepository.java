package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    long countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(Long enrollmentId, Collection<Long> lessonIds);

    /* 내 학습(API-ENR-02) 수업별 상태 계산용 — 한 번의 쿼리로 가져온다 (N+1 방지) */
    List<LessonProgress> findByEnrollmentIdAndLessonIdIn(Long enrollmentId, Collection<Long> lessonIds);

    /*
     * 수업 비공개 영향 확인(API-ADM-32, FR-LSN-014, #118): 이 수업 진도가 있으면서(열어본 적 있음)
     * 수강이 아직 끝나지 않은(완료·포기 안 함) 학습자 수. 코스 전체 수강자 수가 아니라 이 수업에
     * 실제로 영향받는 학습자만 센다.
     */
    @Query("select count(lp) from LessonProgress lp where lp.lessonId = :lessonId and lp.enrollmentId in "
            + "(select e.enrollmentId from Enrollment e where e.completedAt is null and e.droppedAt is null)")
    long countActiveLearnersOf(@Param("lessonId") Long lessonId);
}

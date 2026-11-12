package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

    long countByEnrollmentIdAndLessonIdInAndCompletedAtIsNotNull(Long enrollmentId, Collection<Long> lessonIds);

    /*
     * 기간 내 열린(unlocked_at 있음, unlocked_at 기준) 수업 진도를 수업별로 센 것: [lessonId, 열린 수, 완료 못 한 수].
     * 완료 못 한 수 = 기간이 끝난 시점까지도 completed_at 이 없는 수다 (명세 API-ADM-40). 그래서 종료 시각(to) 뒤에 완료한 것도 센다.
     * from·to 가 null 이면 그 쪽 제한 없음(to 가 null 이면 completed_at 이 없는 것만 센다) (admin 확장 대시보드 수업별 이탈률, FR-ADM-003)
     */
    @Query("select lp.lessonId, count(lp), sum(case when lp.completedAt is null or (:to is not null and lp.completedAt > :to) then 1 else 0 end) "
            + "from LessonProgress lp "
            + "where lp.unlockedAt is not null and (:from is null or lp.unlockedAt >= :from) and (:to is null or lp.unlockedAt <= :to) "
            + "group by lp.lessonId")
    List<Object[]> countDropOffByLessonBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /* 내 학습(API-ENR-02) 수업별 상태 계산용 — 한 번의 쿼리로 가져온다 (N+1 방지) */
    List<LessonProgress> findByEnrollmentIdAndLessonIdIn(Long enrollmentId, Collection<Long> lessonIds);

    /* 활동 추가 때 수업을 끝낸 수강자 찾기(PL-ENR-005) — 수강자마다 조회하지 않고 한 번에 가져온다 (N+1 방지, #476) */
    List<LessonProgress> findByLessonIdAndEnrollmentIdIn(Long lessonId, Collection<Long> enrollmentIds);

    /*
     * 수업 비공개 영향 확인(API-ADM-32, FR-LSN-014, #118): "학습자가 진행 중인 수업"만 센다 — 이 수업을
     * 열었지만(unlocked_at 있음) 아직 끝내지 않았고(completed_at 없음), 수강 자체도 끝나지 않은(완료·포기
     * 안 함) 학습자 수. 공개 수업은 수강 시작·공개 시점에 모든 수강에 진도 행이 미리 생기고 열지 않았으면
     * unlocked_at이 비어 있으므로(lesson_progress 행 존재 자체는 "열어봄"을 뜻하지 않는다), unlocked_at도
     * 같이 검사해야 코스 전체 수강자 수로 되돌아가지 않는다 (Choyeonik 리뷰, #220 머지 후 추가 지적).
     */
    @Query("select count(lp) from LessonProgress lp where lp.lessonId = :lessonId "
            + "and lp.unlockedAt is not null and lp.completedAt is null and lp.enrollmentId in "
            + "(select e.enrollmentId from Enrollment e where e.completedAt is null and e.droppedAt is null)")
    long countActiveLearnersOf(@Param("lessonId") Long lessonId);
}

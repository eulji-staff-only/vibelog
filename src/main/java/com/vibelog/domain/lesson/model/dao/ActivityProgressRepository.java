package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, Long> {

    Optional<ActivityProgress> findByEnrollmentIdAndActivityId(Long enrollmentId, Long activityId);

    /* 수업 화면(API-LSN-01)에서 활동 목록의 열림·완료 상태를 한 번에 읽는다 */
    List<ActivityProgress> findByEnrollmentIdAndActivityIdIn(Long enrollmentId, Collection<Long> activityIds);

    /*
     * 기간 내 열린(unlocked_at 있음, unlocked_at 기준) 활동 진도를 활동별로 센 것: [activityId, 열린 수, 완료 못 한 수].
     * 완료 못 한 수 = 기간이 끝난 시점까지도 completed_at 이 없는 수다 (명세 API-ADM-40). 그래서 종료 시각(to) 뒤에 완료한 것도 센다.
     * from·to 가 null 이면 그 쪽 제한 없음(to 가 null 이면 completed_at 이 없는 것만 센다). 수업별(LessonProgressRepository.countDropOffByLessonBetween)과
     * 같은 기준이다 (admin 확장 대시보드 활동별 이탈률, FR-ADM-003, #364)
     */
    @Query("select ap.activityId, count(ap), sum(case when ap.completedAt is null or (:to is not null and ap.completedAt > :to) then 1 else 0 end) "
            + "from ActivityProgress ap "
            + "where ap.unlockedAt is not null and (:from is null or ap.unlockedAt >= :from) and (:to is null or ap.unlockedAt <= :to) "
            + "group by ap.activityId")
    List<Object[]> countDropOffByActivityBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

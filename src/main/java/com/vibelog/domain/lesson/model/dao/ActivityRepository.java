package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.Activity;
import com.vibelog.domain.lesson.model.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByLessonIdAndIsActiveTrueOrderBySortOrderAsc(Long lessonId);

    /* 운영자 수업 편집 화면(API-ADM-19, #100): 사용 중지된 활동도 함께 보여준다 */
    List<Activity> findByLessonIdOrderBySortOrderAsc(Long lessonId);

    Optional<Activity> findTopByLessonIdAndIsActiveTrueOrderBySortOrderDesc(Long lessonId);

    long countByLessonIdAndActivityTypeAndIsActiveTrue(Long lessonId, ActivityType activityType);

    Optional<Activity> findFirstByLessonIdAndIsActiveTrueOrderBySortOrderAsc(Long lessonId);

    Optional<Activity> findFirstByLessonIdAndIsActiveTrueAndSortOrderGreaterThanOrderBySortOrderAsc(
            Long lessonId, int sortOrder);

    /* 잠긴 활동 선택 시 "앞 활동 바로가기"(FR-LSN-009)에 쓴다 */
    Optional<Activity> findFirstByLessonIdAndIsActiveTrueAndSortOrderLessThanOrderBySortOrderDesc(
            Long lessonId, int sortOrder);

    Optional<Activity> findTopByLessonIdAndActivityTypeAndIsActiveTrueOrderBySortOrderDesc(
            Long lessonId, ActivityType activityType);
}

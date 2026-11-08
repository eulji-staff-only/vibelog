package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, Long> {

    Optional<ActivityProgress> findByEnrollmentIdAndActivityId(Long enrollmentId, Long activityId);

    /* 수업 화면(API-LSN-01)에서 활동 목록의 열림·완료 상태를 한 번에 읽는다 */
    List<ActivityProgress> findByEnrollmentIdAndActivityIdIn(Long enrollmentId, Collection<Long> activityIds);
}

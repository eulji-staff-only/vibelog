package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ActivityProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityProgressRepository extends JpaRepository<ActivityProgress, Long> {

    Optional<ActivityProgress> findByEnrollmentIdAndActivityId(Long enrollmentId, Long activityId);
}

package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ActivityTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityTagRepository extends JpaRepository<ActivityTag, Long> {

    List<ActivityTag> findByActivityId(Long activityId);
}

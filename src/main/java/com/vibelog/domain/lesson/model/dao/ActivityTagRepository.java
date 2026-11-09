package com.vibelog.domain.lesson.model.dao;

import com.vibelog.domain.lesson.model.entity.ActivityTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityTagRepository extends JpaRepository<ActivityTag, Long> {

    List<ActivityTag> findByActivityId(Long activityId);

    /* 활동 수정(API-ADM-22, #101) 시 기존 태그를 지우고 새로 받은 태그로 바꾼다 */
    void deleteByActivityId(Long activityId);
}

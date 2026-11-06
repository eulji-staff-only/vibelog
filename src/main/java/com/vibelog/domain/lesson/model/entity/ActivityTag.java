package com.vibelog.domain.lesson.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ActivityTag]
 * 역할 : 개념 활동의 태그 테이블(activity_tag)과 매핑된다. 개념 활동 하나에 개념 태그를 여러 개 붙인다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 activity_tag 그대로 둔다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityTag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityTagId;

    private Long activityId;

    private Long conceptTagId;

    private ActivityTag(Long activityId, Long conceptTagId) {
        this.activityId = activityId;
        this.conceptTagId = conceptTagId;
    }

    public static ActivityTag create(Long activityId, Long conceptTagId) {
        return new ActivityTag(activityId, conceptTagId);
    }
}

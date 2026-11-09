package com.vibelog.domain.community.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ReviewCriterion]
 * 역할 : 포트폴리오 과제의 평가 기준 (review_criterion). 읽기만 한다.
 * 책임 : 넣는 화면이 없어 DB 팀이 sql/02-seed.sql 로 넣는다 (#209). is_active 인 것만 sort_order 순으로 쓴다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewCriterion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewCriterionId;

    private Long portfolioAssignmentId;

    private String name;

    private String description;

    private int sortOrder;

    private boolean isActive;
}

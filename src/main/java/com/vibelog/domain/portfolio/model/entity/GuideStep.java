package com.vibelog.domain.portfolio.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [GuideStep]
 * 역할 : 과제 스텝 테이블(guide_step)과 매핑된다. 포트폴리오 과제의 스텝 가이드 한 줄.
 * 책임 : 컬럼은 sql/01-schema.sql 의 guide_step 그대로 둔다.
 *        체크 기록이 있는 스텝은 지우지 않고 is_active = FALSE 로 끈다 (DB 설계서 5-1).
 *        만들기·끄기 메서드는 과제 작성 기능(FR-PRT-012)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideStep extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long guideStepId;

    private Long portfolioAssignmentId;

    private String content;

    private int sortOrder;

    private boolean isActive;
}

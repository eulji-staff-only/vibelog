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
 *        순서(sort_order)는 운영자가 보낸 목록 순서다 (API-ADM-35).
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

    public static GuideStep create(Long portfolioAssignmentId, String content, int sortOrder) {
        GuideStep step = new GuideStep();
        step.portfolioAssignmentId = portfolioAssignmentId;
        step.content = content;
        step.sortOrder = sortOrder;
        step.isActive = true;
        return step;
    }

    public void edit(String content, int sortOrder) {
        this.content = content;
        this.sortOrder = sortOrder;
    }

    /* 학습자 체크 기록이 있어 지우지 않고 끈다 (DB 설계서 5-1) */
    public void deactivate() {
        this.isActive = false;
    }
}

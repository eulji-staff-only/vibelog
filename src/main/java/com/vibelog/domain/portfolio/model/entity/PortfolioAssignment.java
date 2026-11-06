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
 * [PortfolioAssignment]
 * 역할 : 포트폴리오 과제 테이블(portfolio_assignment)과 매핑된다. 코스당 과제 1개 (uk_portfolio_assignment_course_id).
 * 책임 : 컬럼은 sql/01-schema.sql 의 portfolio_assignment 그대로 둔다. 코스는 다른 영역이라 ID(Long)로만 가진다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioAssignment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioAssignmentId;

    private Long courseId;

    private String topic;

    private String goal;

    private String agentGuide;          // 평가 에이전트 세팅·평가 기준·실행 방법·"실행이 안 될 때" (FR-PRT-002)

    private String agentFileUrl;        // 선택 (PL-PRT-010)

    public static PortfolioAssignment create(Long courseId, String topic, String goal,
                                             String agentGuide, String agentFileUrl) {
        PortfolioAssignment assignment = new PortfolioAssignment();
        assignment.courseId = courseId;
        assignment.topic = topic;
        assignment.goal = goal;
        assignment.agentGuide = agentGuide;
        assignment.agentFileUrl = agentFileUrl;
        return assignment;
    }
}

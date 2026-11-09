package com.vibelog.domain.portfolio.model.dto;

/*
 * [PortfolioAssignmentAdminView]
 * 역할 : A-07 포트폴리오 과제 편집 화면(API-ADM-34)의 머리 정보.
 * 책임 : - assignmentSaved 가 false 면 아직 과제를 만들지 않은 코스다 (평가 에이전트는 과제를 먼저 저장해야 쓸 수 있다).
 *        - agentFileUrl 은 올려 둔 설정 파일 주소, 없으면 null.
 */
public record PortfolioAssignmentAdminView(Long courseId, String courseTitle, boolean assignmentSaved, String agentFileUrl) {
}

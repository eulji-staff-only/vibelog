package com.vibelog.domain.portfolio.model.dto;

import java.util.List;

/*
 * [PortfolioPageView]
 * 역할 : U-17 포트폴리오 과제 화면(API-PRT-01)에 그릴 값.
 * 책임 : - unlocked 가 false 면 잠김 안내만 그린다. 이때 과제 값은 비어 있고, reportLessonId 는 "리포트 보러 가기"가
 *          열 마지막 수업 ID(공개 수업이 없으면 null)다 (PL-PRT-001).
 *        - submittedPortfolioId 는 그 코스에 제출한 내 포트폴리오(삭제 표시 제외), 없으면 null.
 *        - agentGuide·hasAgentFile 은 제출한 뒤에만 값이 있다. 제출 전(submittedPortfolioId 가 null)에는 null·false (#404).
 *        - emailVerified 가 false 면 제출 영역에 인증 안내를 보여 준다 (PL-USR-003).
 *        - faqs 는 과제 FAQ (P2 FR-PRT-008), sort_order 순. 잠김이거나 FAQ 가 없으면 빈 목록이라 4번 영역을 그리지 않는다.
 */
public record PortfolioPageView(Long courseId, String courseTitle, boolean unlocked, Long reportLessonId,
                                String topic, String goal, String agentGuide, boolean hasAgentFile,
                                List<Step> steps, long checkedCount, Long submittedPortfolioId, boolean emailVerified,
                                List<Faq> faqs) {

    public static PortfolioPageView locked(Long courseId, String courseTitle, Long reportLessonId) {
        return new PortfolioPageView(courseId, courseTitle, false, reportLessonId,
                null, null, null, false, List.of(), 0, null, false, List.of());
    }

    public record Step(Long guideStepId, String content, boolean checked) {
    }

    public record Faq(String question, String answer) {
    }
}

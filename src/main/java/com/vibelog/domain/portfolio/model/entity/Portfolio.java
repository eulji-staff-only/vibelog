package com.vibelog.domain.portfolio.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [Portfolio]
 * 역할 : 포트폴리오 테이블(portfolio)과 매핑된다. 코스 과제를 제출한 결과물 카드다.
 * 책임 : - 컬럼은 sql/01-schema.sql 의 portfolio 그대로 둔다.
 *        - 삭제는 행을 지우지 않고 deleted_at 을 기록한다 (Soft Delete, DB 설계서 5-1).
 *        - 회원·코스는 다른 영역이라 ID(Long)로만 가진다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioId;

    private Long memberId;

    private Long courseId;              // 개인 작품(PERSONAL, P2)은 NULL

    @Enumerated(EnumType.STRING)
    private PortfolioType portfolioType;

    private String title;

    private String resultUrl;

    private String summary;

    private String retrospective;

    private String aiTools;

    private boolean isPublic;           // 기본값 없이 제출할 때 고른다 (PL-PRT-006)

    private String agentResult;         // 선택 (PL-PRT-002)

    private String screenshotUrl;       // 선택, 1장

    private LocalDateTime deletedAt;    // 삭제 표시 (PL-DAT-002)

    /* 코스 과제 포트폴리오를 만든다. 입력 검증·API 키 가리기(PL-PRT-008)는 Service 에서 끝낸 값을 받는다 */
    public static Portfolio create(Long memberId, Long courseId, String title, String resultUrl, String summary,
                                   String retrospective, String aiTools, boolean isPublic,
                                   String agentResult, String screenshotUrl) {
        Portfolio portfolio = new Portfolio();
        portfolio.memberId = memberId;
        portfolio.courseId = courseId;
        portfolio.portfolioType = PortfolioType.COURSE;
        portfolio.title = title;
        portfolio.resultUrl = resultUrl;
        portfolio.summary = summary;
        portfolio.retrospective = retrospective;
        portfolio.aiTools = aiTools;
        portfolio.isPublic = isPublic;
        portfolio.agentResult = agentResult;
        portfolio.screenshotUrl = screenshotUrl;
        return portfolio;
    }

    /*
     * 개인 작품을 만든다 (FR-PRT-011, portfolio.md API-PRT-13). 코스가 없고(course_id NULL, ck_portfolio_type_course),
     * 에이전트 점검 결과·스크린샷은 코스 과제 전용이라 비운다. 입력 검증·API 키 가리기는 Service 에서 끝낸 값을 받는다
     */
    public static Portfolio createPersonal(Long memberId, String title, String resultUrl, String summary,
                                           String retrospective, String aiTools, boolean isPublic) {
        Portfolio portfolio = create(memberId, null, title, resultUrl, summary, retrospective, aiTools, isPublic, null, null);
        portfolio.portfolioType = PortfolioType.PERSONAL;
        return portfolio;
    }

    public boolean isPersonal() {
        return portfolioType == PortfolioType.PERSONAL;
    }

    /*
     * 삭제 표시한 코스 작품을 같은 코스로 다시 제출하면 이 행을 되살려 덮어쓴다 (DB 설계서 5-4, uk_portfolio_member_course 유지).
     * 입력 검증·API 키 가리기는 Service 에서 끝낸 값을 받는다
     */
    public void resubmit(String title, String resultUrl, String summary, String retrospective, String aiTools,
                         boolean isPublic, String agentResult, String screenshotUrl) {
        this.title = title;
        this.resultUrl = resultUrl;
        this.summary = summary;
        this.retrospective = retrospective;
        this.aiTools = aiTools;
        this.isPublic = isPublic;
        this.agentResult = agentResult;
        this.screenshotUrl = screenshotUrl;
        this.deletedAt = null;
    }

    /*
     * 수정 (API-PRT-06, PL-PRT-007): 최신본으로 덮는다. 수정 일시는 BaseTimeEntity 의 updated_at 이 남긴다.
     * 스크린샷은 새로 올렸을 때만 바꾼다 (null 이면 이전 것을 그대로 둔다). 입력 검증·API 키 가리기는 Service 에서 끝낸 값을 받는다
     */
    public void edit(String title, String resultUrl, String summary, String retrospective, String aiTools,
                     boolean isPublic, String agentResult, String screenshotUrl) {
        this.title = title;
        this.resultUrl = resultUrl;
        this.summary = summary;
        this.retrospective = retrospective;
        this.aiTools = aiTools;
        this.isPublic = isPublic;
        this.agentResult = agentResult;
        if (screenshotUrl != null) {
            this.screenshotUrl = screenshotUrl;
        }
    }

    /* 올라가 있는 스크린샷을 지운다 (수정 화면 X 버튼, #437) */
    public void removeScreenshot() {
        this.screenshotUrl = null;
    }

    /* 공개 전환 (API-PRT-08, PL-PRT-006) */
    public void changeVisibility(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    /* 삭제 표시. 이미 삭제 표시된 것은 처음 시각을 그대로 둔다 */
    public void markDeleted() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

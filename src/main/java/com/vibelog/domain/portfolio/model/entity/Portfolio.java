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

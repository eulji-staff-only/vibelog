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
 * [PortfolioFaq]
 * 역할 : 포트폴리오 과제 FAQ 테이블(portfolio_faq)과 매핑된다 (FR-PRT-008·014, P2).
 * 책임 : - 컬럼은 sql/01-schema.sql 의 portfolio_faq 그대로 둔다.
 *        - 운영자가 쓰는 안내 문구라 삭제 표시 없이 행을 지운다 (portfolio.md P2, deleted_at 없음).
 *        - 순서(sort_order)는 운영자가 보낸 목록 순서다 (API-PRT-11).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioFaq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioFaqId;

    private Long portfolioAssignmentId;

    private String question;

    private String answer;

    private int sortOrder;

    public static PortfolioFaq create(Long portfolioAssignmentId, String question, String answer, int sortOrder) {
        PortfolioFaq faq = new PortfolioFaq();
        faq.portfolioAssignmentId = portfolioAssignmentId;
        faq.question = question;
        faq.answer = answer;
        faq.sortOrder = sortOrder;
        return faq;
    }

    public void edit(String question, String answer, int sortOrder) {
        this.question = question;
        this.answer = answer;
        this.sortOrder = sortOrder;
    }
}

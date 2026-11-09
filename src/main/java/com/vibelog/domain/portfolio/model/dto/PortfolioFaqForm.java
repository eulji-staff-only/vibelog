package com.vibelog.domain.portfolio.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/*
 * [PortfolioFaqForm]
 * 역할 : 과제 FAQ 저장 폼 (API-PRT-11, FR-PRT-014).
 * 책임 : - faqs 는 보낸 순서가 곧 FAQ 순서(sort_order)다. 새 항목은 portfolioFaqId 를 비운다.
 *        - 질문 1~300자(portfolio_faq.question VARCHAR(300)), 답 필수 (명세 실패 칸).
 *        - 폼 값을 목록으로 묶어 받으려고 setter 가 있는 클래스로 둔다 (Entity 가 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class PortfolioFaqForm {

    @Valid
    private List<Faq> faqs = new ArrayList<>();

    /* [Faq] 역할 : FAQ 한 줄의 입력 값. 책임 : 새 항목은 portfolioFaqId 를 비운다. */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Faq {
        private Long portfolioFaqId;

        @NotBlank(message = "질문을 입력해 주세요.")
        @Size(max = 300, message = "질문은 300자까지 입력할 수 있습니다.")
        private String question;

        @NotBlank(message = "답을 입력해 주세요.")
        private String answer;

        public Faq(Long portfolioFaqId, String question, String answer) {
            this.portfolioFaqId = portfolioFaqId;
            this.question = question;
            this.answer = answer;
        }
    }

    public static PortfolioFaqForm of(List<Faq> faqs) {
        PortfolioFaqForm form = new PortfolioFaqForm();
        form.faqs = new ArrayList<>(faqs);
        return form;
    }
}

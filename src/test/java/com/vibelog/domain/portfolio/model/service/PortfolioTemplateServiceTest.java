package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.PortfolioTemplateForm;
import com.vibelog.domain.portfolio.model.dto.TemplateItemView;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/*
 * 개인 작품 템플릿 관리 (A-12, API-PRT-14·15, FR-PRT-015, #258). 테스트가 끝나면 롤백된다.
 * 시드 유무에 기대지 않으려고 매 테스트 처음에 템플릿 항목 행을 비운다.
 */
@SpringBootTest
@Transactional
class PortfolioTemplateServiceTest {

    @Autowired PortfolioTemplateService portfolioTemplateService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @BeforeEach
    void clear() {
        jdbcTemplate.update("DELETE FROM portfolio_template_item");
    }

    private static PortfolioTemplateForm form(PortfolioTemplateForm.Item... items) {
        PortfolioTemplateForm form = new PortfolioTemplateForm();
        form.setItems(List.of(items));
        return form;
    }

    private static PortfolioTemplateForm.Item item(String key, String label, String guide) {
        return new PortfolioTemplateForm.Item(key, label, guide);
    }

    private List<TemplateItemView> saved() {
        entityManager.flush();
        entityManager.clear();
        return portfolioTemplateService.getItems();
    }

    @Test
    void 행이_없으면_P2_문구_5개를_기본_순서로_보여_준다() {
        assertThat(portfolioTemplateService.getItems())
                .extracting(TemplateItemView::itemKey, TemplateItemView::label)
                .containsExactly(tuple("TITLE", "제목"), tuple("LINK", "링크"), tuple("DESCRIPTION", "설명"),
                        tuple("AI_TOOLS", "사용한 AI 도구"), tuple("RETROSPECTIVE", "회고"));
    }

    @Test
    void 저장하면_보낸_순서대로_이름과_안내_문구를_남기고_빈_안내는_비운다() {
        portfolioTemplateService.save(form(item("DESCRIPTION", " 한 줄 소개 ", "무엇을 만들었나요?"), item("TITLE", "작품 이름", ""),
                item("LINK", "링크", null), item("AI_TOOLS", "AI 도구", "예: Claude"), item("RETROSPECTIVE", "회고", "배운 점")));

        assertThat(saved()).extracting(TemplateItemView::itemKey, TemplateItemView::label, TemplateItemView::guide)
                .containsExactly(tuple("DESCRIPTION", "한 줄 소개", "무엇을 만들었나요?"), tuple("TITLE", "작품 이름", null),
                        tuple("LINK", "링크", null), tuple("AI_TOOLS", "AI 도구", "예: Claude"), tuple("RETROSPECTIVE", "회고", "배운 점"));
    }

    @Test
    void 다시_저장하면_행을_새로_만들지_않고_고친다() {
        portfolioTemplateService.save(form(item("TITLE", "제목", null), item("LINK", "링크", null), item("DESCRIPTION", "설명", null),
                item("AI_TOOLS", "AI", null), item("RETROSPECTIVE", "회고", null)));
        portfolioTemplateService.save(form(item("RETROSPECTIVE", "돌아보기", null), item("TITLE", "제목", null), item("LINK", "링크", null),
                item("DESCRIPTION", "설명", null), item("AI_TOOLS", "AI", null)));

        assertThat(saved()).extracting(TemplateItemView::itemKey).first().isEqualTo("RETROSPECTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM portfolio_template_item", Integer.class)).isEqualTo(5);
    }

    @Test
    void 항목_키가_빠지거나_겹치거나_모르는_키면_INVALID_INPUT() {
        assertThatThrownBy(() -> portfolioTemplateService.save(form(item("TITLE", "제목", null), item("LINK", "링크", null),
                item("DESCRIPTION", "설명", null), item("AI_TOOLS", "AI", null))))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> portfolioTemplateService.save(form(item("TITLE", "제목", null), item("TITLE", "제목2", null),
                item("DESCRIPTION", "설명", null), item("AI_TOOLS", "AI", null), item("RETROSPECTIVE", "회고", null))))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> portfolioTemplateService.save(form(item("TITLE", "제목", null), item("STACK", "기술 스택", null),
                item("DESCRIPTION", "설명", null), item("AI_TOOLS", "AI", null), item("RETROSPECTIVE", "회고", null))))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.INVALID_INPUT);
    }
}

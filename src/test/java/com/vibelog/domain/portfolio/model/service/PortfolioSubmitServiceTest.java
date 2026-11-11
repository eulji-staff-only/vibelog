package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.PortfolioRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.domain.portfolio.model.entity.Portfolio;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 포트폴리오 제출 (API-PRT-04, PL-PRT-002~004·008, PL-USR-003, PL-SEC-003). 테스트가 끝나면 롤백된다.
 * 열림 여부와 코스 완료는 lesson 계약 ⑥⑦ 실제 구현이 판단한다.
 *
 * 코스 9401: 공개 수업 9401 하나. 회원 9401 = 열림·인증됨·수업 완료, 9402 = 열림·미인증, 9403 = 잠김·인증됨.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname, email_verified_at) VALUES "
                + "(9401, 'pf-sub-a@vibelog.test', '제출A', NOW()), (9402, 'pf-sub-b@vibelog.test', '제출B', NULL), "
                + "(9403, 'pf-sub-c@vibelog.test', '제출C', NOW())",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9401, '테스트트랙', '대상', '설명', 9401)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9401, '테스트분류', 9401)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9401, 9401, 9401, '코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) "
                + "VALUES (9401, 9401, '수업1', '소개', '목표', '내용', 10, 1, TRUE)",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at, portfolio_unlocked_at) VALUES "
                + "(9401, 9401, 9401, NOW(), NOW()), (9402, 9402, 9401, NOW(), NOW()), (9403, 9403, 9401, NOW(), NULL)",
        "INSERT INTO lesson_progress (enrollment_id, lesson_id, unlocked_at, completed_at) VALUES (9401, 9401, NOW(), NOW())",
        "INSERT INTO banned_word (word) VALUES ('금칙어테스트')"
})
class PortfolioSubmitServiceTest {

    static final Long MEMBER = 9401L;
    static final Long COURSE = 9401L;

    @Autowired PortfolioSubmitService portfolioSubmitService;
    @Autowired PortfolioRepository portfolioRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    private PortfolioSubmitForm form(String retrospective) {
        PortfolioSubmitForm form = new PortfolioSubmitForm();
        form.setTitle("할 일 앱");
        form.setResultUrl("https://github.com/me/todo");
        form.setSummary("할 일을 관리하는 앱");
        form.setRetrospective(retrospective);
        form.setAiTools("Claude Code");
        form.setIsPublic(true);
        form.setAgentResult("통과");
        return form;
    }

    private static final String RETRO = "배운 점을 적습니다. ".repeat(12);

    private Portfolio saved() {
        entityManager.flush();
        entityManager.clear();
        return portfolioRepository.findByMemberIdAndCourseId(MEMBER, COURSE).orElseThrow();
    }

    @Test
    void 제출하면_저장하고_API_키를_가리고_코스_완료를_기록한다() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO + " 키 sk-abc123XYZ 를 넣었다"), null);

        Portfolio portfolio = saved();
        assertThat(portfolio.getRetrospective()).contains("sk-****").doesNotContain("abc123XYZ");
        assertThat(portfolio.isPublic()).isTrue();
        assertThat(portfolio.getScreenshotUrl()).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at IS NOT NULL FROM enrollment WHERE enrollment_id = 9401", Boolean.class)).isTrue();
    }

    @Test
    void 과제가_열리지_않았으면_PORTFOLIO_LOCKED() {
        assertThatThrownBy(() -> portfolioSubmitService.submit(9403L, COURSE, form(RETRO), null))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.PORTFOLIO_LOCKED);
    }

    @Test
    void 이메일_인증_전이면_EMAIL_NOT_VERIFIED() {
        assertThatThrownBy(() -> portfolioSubmitService.submit(9402L, COURSE, form(RETRO), null))
                .extracting("errorCode").isEqualTo(CommonErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void 이미_제출했으면_ALREADY_SUBMITTED() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);

        assertThatThrownBy(() -> portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.ALREADY_SUBMITTED);
    }

    @Test
    void 삭제_표시한_포트폴리오가_있으면_그_행을_되살려_덮어쓴다() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);
        Long firstId = saved().getPortfolioId();
        jdbcTemplate.update("UPDATE portfolio SET deleted_at = NOW() WHERE portfolio_id = ?", firstId);
        entityManager.clear();

        PortfolioSubmitForm again = form(RETRO);
        again.setTitle("다시 낸 앱");
        portfolioSubmitService.submit(MEMBER, COURSE, again, null);

        Portfolio portfolio = saved();
        assertThat(portfolio.getPortfolioId()).isEqualTo(firstId);
        assertThat(portfolio.getTitle()).isEqualTo("다시 낸 앱");
        assertThat(portfolio.isDeleted()).isFalse();
    }

    /* 평가 에이전트는 제출 뒤 수정에서 열린다 (#404). 최초 제출에서는 점검 결과를 받지 않는다 */
    @Test
    void 최초_제출은_에이전트_점검_결과가_와도_버린다() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);   // form() 은 agentResult "통과" 를 담고 있다

        assertThat(saved().getAgentResult()).isNull();
    }

    @Test
    void 삭제_뒤_다시_제출해도_이전_점검_결과는_남지_않는다() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);
        Long id = saved().getPortfolioId();
        portfolioSubmitService.edit(MEMBER, id, form(RETRO), null);
        assertThat(saved().getAgentResult()).isEqualTo("통과");
        jdbcTemplate.update("UPDATE portfolio SET deleted_at = NOW() WHERE portfolio_id = ?", id);
        entityManager.clear();

        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);

        assertThat(saved().getAgentResult()).isNull();
    }

    @Test
    void 수정에서는_에이전트_점검_결과를_저장한다() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);
        Long id = saved().getPortfolioId();

        PortfolioSubmitForm edited = form(RETRO);
        edited.setAgentResult("에이전트 점검 통과 sk-abc123XYZ");
        portfolioSubmitService.edit(MEMBER, id, edited, null);

        assertThat(saved().getAgentResult()).contains("sk-****").doesNotContain("abc123XYZ");
    }

    @Test
    void 수정할_때_에이전트_점검_결과가_저장_칸보다_길면_INVALID_INPUT() {
        portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO), null);
        Long id = saved().getPortfolioId();
        PortfolioSubmitForm form = form(RETRO);
        form.setAgentResult("가".repeat(21_846));   // 한글 3바이트 × 21,846 = 65,538바이트 > TEXT 65,535

        assertThatThrownBy(() -> portfolioSubmitService.edit(MEMBER, id, form, null))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.INVALID_INPUT);
    }

    @Test
    void 금칙어가_들어_있으면_BANNED_WORD() {
        assertThatThrownBy(() -> portfolioSubmitService.submit(MEMBER, COURSE, form(RETRO + "금칙어테스트"), null))
                .extracting("errorCode").isEqualTo(CommonErrorCode.BANNED_WORD);
    }
}

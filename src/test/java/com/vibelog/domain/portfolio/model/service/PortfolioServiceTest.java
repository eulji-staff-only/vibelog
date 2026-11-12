package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.GuideStepCheckResult;
import com.vibelog.domain.portfolio.model.dto.PortfolioPageView;
import com.vibelog.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/*
 * 포트폴리오 과제 화면·스텝 체크·설정 파일 (API-PRT-01~03, PL-PRT-001).
 * 열림 여부는 lesson 계약 ⑥ 실제 구현이 enrollment.portfolio_unlocked_at 으로 판단한다 (테스트가 끝나면 롤백).
 *
 * 코스 9201: 공개 수업 9201·9202, 비공개 수업 9203. 과제 9201의 스텝 9201·9202(사용 중), 9203(꺼짐).
 * 회원 9201 = 열림·인증됨, 9202 = 수강 중이지만 잠김, 9203 = 수강 안 함.
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname, email_verified_at) VALUES "
                + "(9201, 'pf-svc-a@vibelog.test', '포트A', NOW()), (9202, 'pf-svc-b@vibelog.test', '포트B', NULL), "
                + "(9203, 'pf-svc-c@vibelog.test', '포트C', NULL)",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9201, '테스트트랙', '대상', '설명', 9201)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9201, '테스트분류', 9201)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (9201, 9201, 9201, '바이브 코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO lesson (lesson_id, course_id, title, intro, goal, contents, estimated_minutes, sort_order, is_public) VALUES "
                + "(9201, 9201, '수업1', '소개', '목표', '내용', 10, 1, TRUE), (9202, 9201, '수업2', '소개', '목표', '내용', 10, 2, TRUE), "
                + "(9203, 9201, '수업3', '소개', '목표', '내용', 10, 3, FALSE)",
        "INSERT INTO enrollment (member_id, course_id, started_at, portfolio_unlocked_at) VALUES "
                + "(9201, 9201, NOW(), NOW()), (9202, 9201, NOW(), NULL)",
        "INSERT INTO portfolio_assignment (portfolio_assignment_id, course_id, topic, goal, agent_guide, agent_file_url) "
                + "VALUES (9201, 9201, '할 일 앱 만들기', '목표', '세팅 방법', NULL)",
        "INSERT INTO guide_step (guide_step_id, portfolio_assignment_id, content, sort_order, is_active) VALUES "
                + "(9202, 9201, '둘째 스텝', 2, TRUE), (9201, 9201, '첫 스텝', 1, TRUE), (9203, 9201, '꺼진 스텝', 3, FALSE)"
})
class PortfolioServiceTest {

    static final Long OPEN = 9201L;
    static final Long LOCKED = 9202L;
    static final Long NOT_ENROLLED = 9203L;
    static final Long COURSE = 9201L;

    @Autowired PortfolioService portfolioService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    @Test
    void 수강하지_않은_코스면_화면_값이_없다() {
        assertThat(portfolioService.getPage(NOT_ENROLLED, COURSE)).isEmpty();
    }

    @Test
    void 잠겼으면_과제_없이_마지막_공개_수업으로_안내한다() {
        PortfolioPageView page = portfolioService.getPage(LOCKED, COURSE).orElseThrow();

        assertThat(page.unlocked()).isFalse();
        assertThat(page.topic()).isNull();
        assertThat(page.steps()).isEmpty();
        assertThat(page.reportLessonId()).isEqualTo(9202L);
        assertThat(page.courseTitle()).isEqualTo("바이브 코스");
    }

    @Test
    void 열렸으면_사용_중인_스텝만_순서대로_내_체크와_함께_보여_준다() {
        portfolioService.check(OPEN, 9202L, true);

        PortfolioPageView page = portfolioService.getPage(OPEN, COURSE).orElseThrow();

        assertThat(page.unlocked()).isTrue();
        assertThat(page.topic()).isEqualTo("할 일 앱 만들기");
        assertThat(page.steps()).extracting(PortfolioPageView.Step::guideStepId, PortfolioPageView.Step::checked)
                .containsExactly(tuple(9201L, false), tuple(9202L, true));
        assertThat(page.checkedCount()).isEqualTo(1);
        assertThat(page.emailVerified()).isTrue();
        assertThat(page.submittedPortfolioId()).isNull();
    }

    @Test
    void FAQ는_과제가_열렸을_때만_순서대로_보여_준다() {
        jdbcTemplate.update("INSERT INTO portfolio_faq (portfolio_assignment_id, question, answer, sort_order) VALUES "
                + "(9201, '두 번째 질문', '두 번째 답', 2), (9201, '첫 질문', '첫 답', 1)");

        assertThat(portfolioService.getPage(OPEN, COURSE).orElseThrow().faqs())
                .extracting(PortfolioPageView.Faq::question, PortfolioPageView.Faq::answer)
                .containsExactly(tuple("첫 질문", "첫 답"), tuple("두 번째 질문", "두 번째 답"));
        assertThat(portfolioService.getPage(LOCKED, COURSE).orElseThrow().faqs()).isEmpty();
    }

    @Test
    void 체크는_두_번_보내도_한_줄이고_풀면_지운다() {
        portfolioService.check(OPEN, 9201L, true);
        GuideStepCheckResult twice = portfolioService.check(OPEN, 9201L, true);

        assertThat(twice).isEqualTo(new GuideStepCheckResult(1, 2));
        assertThat(checkRows()).isEqualTo(1);

        GuideStepCheckResult unchecked = portfolioService.check(OPEN, 9201L, false);

        assertThat(unchecked).isEqualTo(new GuideStepCheckResult(0, 2));
        assertThat(checkRows()).isZero();
    }

    @Test
    void 잠겼거나_수강하지_않았으면_체크할_수_없다() {
        assertThatThrownBy(() -> portfolioService.check(LOCKED, 9201L, true))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.PORTFOLIO_LOCKED);
        assertThatThrownBy(() -> portfolioService.check(NOT_ENROLLED, 9201L, true))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.PORTFOLIO_LOCKED);
    }

    @Test
    void 꺼진_스텝은_체크할_수_없다() {
        assertThatThrownBy(() -> portfolioService.check(OPEN, 9203L, true))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }

    /* 평가 에이전트는 포트폴리오를 제출한 뒤에 열린다 (#404) */
    @Test
    void 제출하기_전에는_에이전트_안내와_설정_파일_값을_주지_않는다() {
        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = '/uploads/agent.md' WHERE portfolio_assignment_id = 9201");
        entityManager.clear();

        PortfolioPageView page = portfolioService.getPage(OPEN, COURSE).orElseThrow();

        assertThat(page.unlocked()).isTrue();
        assertThat(page.agentGuide()).isNull();
        assertThat(page.hasAgentFile()).isFalse();
    }

    @Test
    void 제출한_뒤에는_에이전트_안내와_설정_파일_값을_준다() {
        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = '/uploads/agent.md' WHERE portfolio_assignment_id = 9201");
        entityManager.clear();
        submitPortfolio();

        PortfolioPageView page = portfolioService.getPage(OPEN, COURSE).orElseThrow();

        assertThat(page.agentGuide()).isEqualTo("세팅 방법");
        assertThat(page.hasAgentFile()).isTrue();
    }

    @Test
    void 제출하기_전에는_주소를_직접_열어도_설정_파일을_받을_수_없다() {
        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = '/uploads/agent.md' WHERE portfolio_assignment_id = 9201");
        entityManager.clear();

        assertThatThrownBy(() -> portfolioService.getAgentFileUrl(OPEN, COURSE))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.AGENT_NOT_OPEN);
    }

    @Test
    void 제출한_포트폴리오를_지우면_에이전트는_다시_닫힌다() {
        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = '/uploads/agent.md' WHERE portfolio_assignment_id = 9201");
        entityManager.clear();
        submitPortfolio();
        jdbcTemplate.update("UPDATE portfolio SET deleted_at = NOW() WHERE member_id = 9201 AND course_id = 9201");
        entityManager.clear();

        assertThat(portfolioService.getPage(OPEN, COURSE).orElseThrow().agentGuide()).isNull();
        assertThatThrownBy(() -> portfolioService.getAgentFileUrl(OPEN, COURSE))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.AGENT_NOT_OPEN);
    }

    @Test
    void 설정_파일은_제출했고_올려_둔_파일이_있을_때만_내려받는다() {
        submitPortfolio();
        assertThatThrownBy(() -> portfolioService.getAgentFileUrl(OPEN, COURSE))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.FILE_NOT_FOUND);

        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = '/uploads/agent.md' WHERE portfolio_assignment_id = 9201");
        entityManager.clear();   // 앞에서 읽어 둔 과제를 버리고 DB 값을 다시 읽는다

        assertThat(portfolioService.getAgentFileUrl(OPEN, COURSE)).isEqualTo("/uploads/agent.md");
        assertThatThrownBy(() -> portfolioService.getAgentFileUrl(LOCKED, COURSE))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.PORTFOLIO_LOCKED);
    }

    /* 회원 9201 이 코스 9201 에 포트폴리오를 낸 상태로 만든다 */
    private void submitPortfolio() {
        jdbcTemplate.update("INSERT INTO portfolio (member_id, course_id, title, result_url, summary, retrospective, ai_tools, is_public) "
                + "VALUES (9201, 9201, '내 작품', 'https://github.com/a/app', '요약', '회고', 'Claude Code', TRUE)");
        entityManager.clear();
    }

    private Integer checkRows() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM guide_step_check WHERE member_id = 9201", Integer.class);
    }
}

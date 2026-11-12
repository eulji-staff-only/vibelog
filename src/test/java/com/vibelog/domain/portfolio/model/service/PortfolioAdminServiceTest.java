package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dao.GuideStepRepository;
import com.vibelog.domain.portfolio.model.dao.PortfolioAssignmentRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioAssignmentForm;
import com.vibelog.domain.portfolio.model.entity.PortfolioFaq;
import com.vibelog.domain.portfolio.model.dao.PortfolioFaqRepository;
import com.vibelog.domain.portfolio.model.dto.PortfolioFaqForm;
import com.vibelog.domain.portfolio.model.entity.GuideStep;
import com.vibelog.domain.portfolio.model.entity.PortfolioAssignment;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/*
 * 운영자 포트폴리오 과제 편집 규칙 (API-ADM-35·36, FR-PRT-012·013).
 * 핵심 규칙: 학습자가 체크한 스텝은 목록에서 빠져도 지우지 않고 끈다 (#104 완료 조건).
 * 테스트 데이터는 테스트 트랜잭션 안에서 넣고 끝나면 롤백된다 (ID 는 9200번대).
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname) VALUES (9201, 'pf-admin@vibelog.test', '체크한학습자')",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9201, '테스트트랙', '대상', '설명', 9201)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9201, '테스트분류', 9201)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES "
                + "(9201, 9201, 9201, '코스A', '요약', '설명', '대상', '목표', 7), (9202, 9201, 9201, '코스B', '요약', '설명', '대상', '목표', 7)"
})
class PortfolioAdminServiceTest {

    static final Long COURSE = 9201L;
    static final Long OTHER_COURSE = 9202L;

    @Autowired PortfolioAdminService portfolioAdminService;
    @Autowired PortfolioAssignmentRepository portfolioAssignmentRepository;
    @Autowired GuideStepRepository guideStepRepository;
    @Autowired PortfolioFaqRepository portfolioFaqRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    private static PortfolioAssignmentForm form(String topic, PortfolioAssignmentForm.Step... steps) {
        return PortfolioAssignmentForm.of(topic, "목표", List.of(steps));
    }

    private static PortfolioAssignmentForm.Step step(Long id, String content) {
        return new PortfolioAssignmentForm.Step(id, content);
    }

    private PortfolioAssignment assignment() {
        return portfolioAssignmentRepository.findByCourseId(COURSE).orElseThrow();
    }

    private List<GuideStep> activeSteps() {
        entityManager.flush();
        entityManager.clear();
        return guideStepRepository.findByPortfolioAssignmentIdAndIsActiveTrueOrderBySortOrderAsc(
                assignment().getPortfolioAssignmentId());
    }

    @Test
    void 과제가_없으면_만들고_스텝을_보낸_순서대로_저장한다() {
        portfolioAdminService.saveAssignment(COURSE, form("주제", step(null, "첫째"), step(null, "둘째")));

        assertThat(assignment().getTopic()).isEqualTo("주제");
        assertThat(activeSteps()).extracting(GuideStep::getContent, GuideStep::getSortOrder)
                .containsExactly(tuple("첫째", 1), tuple("둘째", 2));
    }

    @Test
    void 다시_저장하면_고치고_순서를_바꾸고_체크_없는_빠진_스텝은_지운다() {
        portfolioAdminService.saveAssignment(COURSE, form("주제", step(null, "A"), step(null, "B"), step(null, "C")));
        List<GuideStep> saved = activeSteps();
        Long a = saved.get(0).getGuideStepId();
        Long c = saved.get(2).getGuideStepId();
        Long b = saved.get(1).getGuideStepId();

        portfolioAdminService.saveAssignment(COURSE, form("새 주제", step(c, "C 고침"), step(a, "A"), step(null, "D")));

        assertThat(assignment().getTopic()).isEqualTo("새 주제");
        assertThat(activeSteps()).extracting(GuideStep::getContent).containsExactly("C 고침", "A", "D");
        assertThat(guideStepRepository.findById(b)).isEmpty();
    }

    @Test
    void 학습자가_체크한_스텝을_빼면_지우지_않고_비활성으로_남긴다() {
        portfolioAdminService.saveAssignment(COURSE, form("주제", step(null, "체크됨"), step(null, "남김")));
        List<GuideStep> saved = activeSteps();
        Long checked = saved.get(0).getGuideStepId();
        jdbcTemplate.update("INSERT INTO guide_step_check (member_id, guide_step_id, checked_at) VALUES (9201, ?, NOW())", checked);

        portfolioAdminService.saveAssignment(COURSE, form("주제", step(saved.get(1).getGuideStepId(), "남김")));

        assertThat(activeSteps()).extracting(GuideStep::getContent).containsExactly("남김");
        assertThat(guideStepRepository.findById(checked)).get()
                .satisfies(step -> assertThat(step.isActive()).isFalse());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM guide_step_check WHERE guide_step_id = ?", Integer.class, checked)).isEqualTo(1);
    }

    @Test
    void 다른_과제의_스텝_ID를_보내면_INVALID_INPUT() {
        portfolioAdminService.saveAssignment(OTHER_COURSE, form("다른 코스", step(null, "남의 스텝")));
        entityManager.flush();
        Long otherStep = guideStepRepository.findByPortfolioAssignmentIdAndIsActiveTrueOrderBySortOrderAsc(
                portfolioAssignmentRepository.findByCourseId(OTHER_COURSE).orElseThrow().getPortfolioAssignmentId())
                .get(0).getGuideStepId();

        assertThatThrownBy(() -> portfolioAdminService.saveAssignment(COURSE, form("주제", step(otherStep, "가로채기"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.INVALID_INPUT);
    }

    @Test
    void 없는_코스는_COURSE_NOT_FOUND() {
        assertThatThrownBy(() -> portfolioAdminService.getView(999_999L))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.COURSE_NOT_FOUND);
        assertThatThrownBy(() -> portfolioAdminService.saveAssignment(999_999L, form("주제")))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.COURSE_NOT_FOUND);
    }

    @Test
    void 평가_에이전트는_과제를_먼저_저장해야_쓸_수_있다() {
        assertThatThrownBy(() -> portfolioAdminService.saveAgent(COURSE, "안내", null))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.INVALID_INPUT);
    }

    @Test
    void 새_설정_파일이_없으면_올려_둔_파일을_그대로_둔다() {
        portfolioAdminService.saveAssignment(COURSE, form("주제"));
        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = '/uploads/old.md' WHERE course_id = ?", COURSE);
        entityManager.clear();

        portfolioAdminService.saveAgent(COURSE, "새 안내", new MockMultipartFile("agentFile", new byte[0]));

        entityManager.flush();
        entityManager.clear();
        assertThat(assignment().getAgentGuide()).isEqualTo("새 안내");
        assertThat(assignment().getAgentFileUrl()).isEqualTo("/uploads/old.md");
    }

    @Test
    void 실행_파일은_설정_파일로_올릴_수_없다() {
        portfolioAdminService.saveAssignment(COURSE, form("주제"));
        MockMultipartFile script = new MockMultipartFile("agentFile", "run.sh", "text/plain", "#!/bin/sh".getBytes());

        assertThatThrownBy(() -> portfolioAdminService.saveAgent(COURSE, "안내", script))
                .extracting("errorCode").isEqualTo(CommonErrorCode.INVALID_FILE);
    }
    private static PortfolioFaqForm faqs(PortfolioFaqForm.Faq... faqs) {
        return PortfolioFaqForm.of(List.of(faqs));
    }

    private List<PortfolioFaq> savedFaqs(Long courseId) {
        entityManager.flush();
        entityManager.clear();
        return portfolioFaqRepository.findByPortfolioAssignmentIdOrderBySortOrderAsc(
                portfolioAssignmentRepository.findByCourseId(courseId).orElseThrow().getPortfolioAssignmentId());
    }

    @Test
    void FAQ는_보낸_목록으로_맞춘다_고치고_순서를_바꾸고_새로_넣고_빠진_것은_지운다() {
        portfolioAdminService.saveAssignment(COURSE, form("주제"));
        portfolioAdminService.saveFaqs(COURSE, faqs(new PortfolioFaqForm.Faq(null, "A", "a"), new PortfolioFaqForm.Faq(null, "B", "b"),
                new PortfolioFaqForm.Faq(null, "C", "c")));
        List<PortfolioFaq> saved = savedFaqs(COURSE);

        portfolioAdminService.saveFaqs(COURSE, faqs(new PortfolioFaqForm.Faq(saved.get(2).getPortfolioFaqId(), "C 고침", "c"),
                new PortfolioFaqForm.Faq(null, "새 D", "d"), new PortfolioFaqForm.Faq(saved.get(0).getPortfolioFaqId(), "A", "a")));

        assertThat(savedFaqs(COURSE)).extracting(PortfolioFaq::getQuestion, PortfolioFaq::getSortOrder)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("C 고침", 1), org.assertj.core.groups.Tuple.tuple("새 D", 2),
                        org.assertj.core.groups.Tuple.tuple("A", 3));
        assertThat(portfolioFaqRepository.findById(saved.get(1).getPortfolioFaqId())).isEmpty();
    }

    @Test
    void FAQ는_과제를_먼저_저장해야_하고_다른_과제의_FAQ_ID면_INVALID_INPUT() {
        assertThatThrownBy(() -> portfolioAdminService.saveFaqs(COURSE, faqs(new PortfolioFaqForm.Faq(null, "Q", "A"))))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.INVALID_INPUT);

        portfolioAdminService.saveAssignment(OTHER_COURSE, form("다른 코스"));
        portfolioAdminService.saveFaqs(OTHER_COURSE, faqs(new PortfolioFaqForm.Faq(null, "남의 질문", "답")));
        Long otherFaq = savedFaqs(OTHER_COURSE).get(0).getPortfolioFaqId();
        portfolioAdminService.saveAssignment(COURSE, form("주제"));

        assertThatThrownBy(() -> portfolioAdminService.saveFaqs(COURSE, faqs(new PortfolioFaqForm.Faq(otherFaq, "가로채기", "답"))))
                .extracting("errorCode").isEqualTo(PortfolioErrorCode.INVALID_INPUT);
    }

    @Test
    void 저장된_FAQ로_FAQ_탭_폼을_채우고_과제가_없으면_빈_폼이다() {
        assertThat(portfolioAdminService.getFaqForm(COURSE).getFaqs()).isEmpty();
        portfolioAdminService.saveAssignment(COURSE, form("주제"));
        portfolioAdminService.saveFaqs(COURSE, faqs(new PortfolioFaqForm.Faq(null, "질문", "답")));
        entityManager.flush();

        assertThat(portfolioAdminService.getFaqForm(COURSE).getFaqs())
                .extracting(PortfolioFaqForm.Faq::getQuestion).containsExactly("질문");
    }
}

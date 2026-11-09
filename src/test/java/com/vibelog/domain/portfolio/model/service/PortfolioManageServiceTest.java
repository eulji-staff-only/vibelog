package com.vibelog.domain.portfolio.model.service;

import com.vibelog.domain.portfolio.exception.PortfolioErrorCode;
import com.vibelog.domain.portfolio.model.dto.CoursePortfolioCard;
import com.vibelog.domain.portfolio.model.dto.MyPortfolioCard;
import com.vibelog.domain.portfolio.model.dto.PortfolioDetailView;
import com.vibelog.domain.portfolio.model.dto.PortfolioSubmitForm;
import com.vibelog.global.exception.CommonErrorCode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 내 포트폴리오·상세·삭제·공개 전환·수정 (API-PRT-05~09, PL-ENR-003, PL-PRT-007·008, PL-ERR-006). 테스트가 끝나면 롤백된다.
 * 삭제 때 코스 완료 일시 비우기는 lesson 계약 ⑦ 실제 구현이 한다.
 *
 * 코스 8801. 회원 8801(인증됨, 코스 완료) = 비공개 포트폴리오 8801 (스크린샷 있음). 회원 8802 = 공개 포트폴리오 8802.
 * 포트폴리오 8802 에는 열린 리뷰 요청 8802 가 있다 (계약 ⑭ 확인용).
 */
@SpringBootTest
@Transactional
@Sql(statements = {
        "INSERT INTO member (member_id, email, nickname, email_verified_at) VALUES "
                + "(8801, 'pf-my-a@vibelog.test', '작성자A', NOW()), (8802, 'pf-my-b@vibelog.test', '작성자B', NOW())",
        "INSERT INTO track (track_id, name, target, description, sort_order) VALUES (8801, '테스트트랙', '대상', '설명', 8801)",
        "INSERT INTO course_category (course_category_id, name, sort_order) VALUES (8801, '테스트분류', 8801)",
        "INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (8801, 8801, 8801, '바이브 코스', '요약', '설명', '대상', '목표', 7)",
        "INSERT INTO enrollment (enrollment_id, member_id, course_id, started_at, portfolio_unlocked_at, completed_at) VALUES "
                + "(8801, 8801, 8801, NOW(), NOW(), NOW()), (8802, 8802, 8801, NOW(), NOW(), NOW())",
        "INSERT INTO portfolio (portfolio_id, member_id, course_id, title, result_url, summary, retrospective, ai_tools, is_public, screenshot_url) VALUES "
                + "(8801, 8801, 8801, '내 작품', 'https://github.com/a/app', '내 요약', '내 회고', 'Claude Code', FALSE, '/uploads/shot.png'), "
                + "(8802, 8802, 8801, '남의 작품', 'https://github.com/b/app', '남 요약', '남 회고', 'Cursor', TRUE, NULL)",
        "INSERT INTO review_request (review_request_id, portfolio_id, requested_at) VALUES (8802, 8802, NOW())"
})
class PortfolioManageServiceTest {

    static final Long ME = 8801L;
    static final Long OTHER = 8802L;
    static final Long MINE = 8801L;
    static final Long OTHERS = 8802L;

    @Autowired PortfolioManageService portfolioManageService;
    @Autowired PortfolioSubmitService portfolioSubmitService;
    @Autowired PortfolioContractService portfolioContractService;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired EntityManager entityManager;

    private Map<String, Object> row(Long portfolioId) {
        entityManager.flush();
        return jdbcTemplate.queryForMap("SELECT * FROM portfolio WHERE portfolio_id = ?", portfolioId);
    }

    @Test
    void 내_포트폴리오는_코스_이름과_함께_내_것만_보여_준다() {
        assertThat(portfolioManageService.getMyPortfolios(ME))
                .extracting(MyPortfolioCard::portfolioId, MyPortfolioCard::courseTitle, MyPortfolioCard::isPublic)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(MINE, "바이브 코스", false));
    }

    @Test
    void 비공개_상세는_본인만_보고_남이면_비공개된_작품이다() {
        PortfolioDetailView mine = portfolioManageService.getDetail(ME, MINE);
        assertThat(mine.mine()).isTrue();
        assertThat(mine.authorNickname()).isEqualTo("작성자A");

        assertThatThrownBy(() -> portfolioManageService.getDetail(OTHER, MINE))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PRIVATE_PORTFOLIO);
    }

    @Test
    void 공개_상세는_다른_회원도_본다() {
        PortfolioDetailView others = portfolioManageService.getDetail(ME, OTHERS);
        assertThat(others.mine()).isFalse();
        assertThat(others.courseTitle()).isEqualTo("바이브 코스");
    }

    @Test
    void 삭제하면_삭제_표시하고_그_코스가_다시_미완료가_된다() {
        portfolioManageService.delete(ME, MINE);

        assertThat(row(MINE).get("deleted_at")).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at IS NULL FROM enrollment WHERE enrollment_id = 8801", Boolean.class)).isTrue();
        assertThat(portfolioManageService.getMyPortfolios(ME)).isEmpty();
        assertThatThrownBy(() -> portfolioManageService.getDetail(ME, MINE))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_NOT_FOUND);
    }

    @Test
    void 남의_포트폴리오는_삭제_공개_전환_수정할_수_없다() {
        assertThatThrownBy(() -> portfolioManageService.delete(ME, OTHERS))
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_OWNER);
        assertThatThrownBy(() -> portfolioManageService.changeVisibility(ME, OTHERS, false))
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_OWNER);
        assertThatThrownBy(() -> portfolioSubmitService.edit(ME, OTHERS, form(), null))
                .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_OWNER);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT completed_at IS NOT NULL FROM enrollment WHERE enrollment_id = 8802", Boolean.class)).isTrue();
    }

    private boolean reviewRequestClosed() {
        entityManager.flush();
        return jdbcTemplate.queryForObject(
                "SELECT closed_at IS NOT NULL FROM review_request WHERE review_request_id = 8802", Boolean.class);
    }

    @Test
    void 비공개로_바꾸면_열린_리뷰_요청을_닫는다() {
        portfolioManageService.changeVisibility(OTHER, OTHERS, false);
        assertThat(reviewRequestClosed()).isTrue();
    }

    @Test
    void 공개로_바꾸면_리뷰_요청은_그대로다() {
        portfolioManageService.changeVisibility(OTHER, OTHERS, true);
        assertThat(reviewRequestClosed()).isFalse();
    }

    @Test
    void 삭제하면_열린_리뷰_요청을_닫는다() {
        portfolioManageService.delete(OTHER, OTHERS);
        assertThat(reviewRequestClosed()).isTrue();
    }

    @Test
    void 비공개로_수정하면_열린_리뷰_요청을_닫는다() {
        PortfolioSubmitForm form = form();
        form.setIsPublic(false);

        portfolioSubmitService.edit(OTHER, OTHERS, form, null);

        assertThat(reviewRequestClosed()).isTrue();
    }

    @Test
    void 공개_전환은_바뀐_값을_저장하고_돌려준다() {
        assertThat(portfolioManageService.changeVisibility(ME, MINE, true)).isTrue();
        assertThat(row(MINE).get("is_public")).isEqualTo(true);
    }

    @Test
    void 수정하면_최신본으로_덮고_API_키를_가리고_스크린샷은_그대로_둔다() {
        PortfolioSubmitForm form = form();
        form.setRetrospective(form.getRetrospective() + " 키 ghp_abcDEF123 를 적었다");

        portfolioSubmitService.edit(ME, MINE, form, null);

        Map<String, Object> row = row(MINE);
        assertThat(row.get("title")).isEqualTo("고친 제목");
        assertThat(row.get("is_public")).isEqualTo(true);
        assertThat((String) row.get("retrospective")).contains("ghp_****").doesNotContain("abcDEF123");
        assertThat(row.get("screenshot_url")).isEqualTo("/uploads/shot.png");
    }

    @Test
    void 수정_상태_폼은_저장된_값으로_채운다() {
        PortfolioSubmitForm form = portfolioManageService.getEditForm(ME, MINE);
        assertThat(form.getTitle()).isEqualTo("내 작품");
        assertThat(form.getIsPublic()).isFalse();
    }

    /*
     * 같은 코스 작품 (API-PRT-10, PL-PRT-011, PL-RPT-006). 기본 데이터에 더해
     * 8803 = 공개지만 영구 정지 회원, 8804 = 공개지만 삭제 표시, 8805 = 다른 코스(8802)의 공개 작품, 회원 8806 = 포트폴리오 없음.
     */
    private void givenCoursePortfolios() {
        jdbcTemplate.update("INSERT INTO member (member_id, email, nickname) VALUES (8803, 'pf-c3@vibelog.test', '정지'), "
                + "(8804, 'pf-c4@vibelog.test', '삭제'), (8805, 'pf-c5@vibelog.test', '다른코스'), (8806, 'pf-c6@vibelog.test', '안냄')");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) "
                + "VALUES (8802, 8801, 8801, '다른 코스', '요약', '설명', '대상', '목표', 7)");
        jdbcTemplate.update("INSERT INTO portfolio (portfolio_id, member_id, course_id, title, result_url, summary, retrospective, ai_tools, is_public, deleted_at) VALUES "
                + "(8803, 8803, 8801, '정지 작품', 'https://a.io/x', '요약', '회고', '도구', TRUE, NULL), "
                + "(8804, 8804, 8801, '삭제 작품', 'https://a.io/x', '요약', '회고', '도구', TRUE, NOW()), "
                + "(8805, 8805, 8802, '다른 코스 작품', 'https://a.io/x', '요약', '회고', '도구', TRUE, NULL)");
        jdbcTemplate.update("INSERT INTO member_suspension (member_id, admin_member_id, level, reason, starts_at) "
                + "VALUES (8803, 8802, 'PERMANENT', '테스트', NOW())");
    }

    @Test
    void 같은_코스_작품은_그_코스의_공개_작품만_작성자와_함께_보여_준다() {
        givenCoursePortfolios();

        assertThat(portfolioManageService.getCoursePortfolios(ME, 8801L, 1).portfolios().getContent())
                .extracting(CoursePortfolioCard::portfolioId, CoursePortfolioCard::authorNickname)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(OTHERS, "작성자B"));
    }

    @Test
    void 그_코스에_포트폴리오를_내지_않았거나_지웠으면_같은_코스_작품을_볼_수_없다() {
        givenCoursePortfolios();
        assertThatThrownBy(() -> portfolioManageService.getCoursePortfolios(8806L, 8801L, 1))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_NOT_SUBMITTED);

        portfolioManageService.delete(ME, MINE);
        assertThatThrownBy(() -> portfolioManageService.getCoursePortfolios(ME, 8801L, 1))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.PORTFOLIO_NOT_SUBMITTED);
    }

    @Test
    void 없는_코스면_찾을_수_없다() {
        assertThatThrownBy(() -> portfolioManageService.getCoursePortfolios(ME, 8899L, 1))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.COURSE_NOT_FOUND);
    }

    /* ---------- #314 영구 정지 작성자 숨김 (PL-RPT-006) ---------- */

    private void suspendOther(String level, String releasedAt) {
        jdbcTemplate.update("INSERT INTO member_suspension (member_id, admin_member_id, level, reason, starts_at, ends_at, released_at) "
                + "VALUES (8802, 8801, ?, '테스트', NOW(), " + ("PERMANENT".equals(level) ? "NULL" : "DATE_ADD(NOW(), INTERVAL 7 DAY)")
                + ", " + releasedAt + ")", level);
    }

    @Test
    void 작성자가_영구_정지면_다른_회원에게_상세를_숨기고_본인에게는_보인다() {
        suspendOther("PERMANENT", "NULL");

        assertThatThrownBy(() -> portfolioManageService.getDetail(ME, OTHERS))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.HIDDEN_CONTENT);
        assertThat(portfolioManageService.getDetail(OTHER, OTHERS).mine()).isTrue();
    }

    @Test
    void 영구_정지를_해제했거나_기간_정지면_상세가_그대로_보인다() {
        suspendOther("PERMANENT", "NOW()");
        suspendOther("DAYS_7", "NULL");

        assertThat(portfolioManageService.getDetail(ME, OTHERS).title()).isEqualTo("남의 작품");
    }

    /* ---------- #314 콘테스트 계약 ⑮⑯ (PL-PRT-007, PL-CTS-006) ---------- */

    /* 포트폴리오 8802 가 투표 중인 콘테스트에 출품되어 있다 (투표는 내일 끝남) */
    private void enterContest() {
        jdbcTemplate.update("INSERT INTO contest (contest_id, contest_month, topic, description, entry_starts_at, entry_ends_at, vote_starts_at, vote_ends_at) "
                + "VALUES (8801, '2999-01', '주제', '설명', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), "
                + "DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY))");
        jdbcTemplate.update("INSERT INTO contest_entry (contest_entry_id, contest_id, member_id, portfolio_id) VALUES (8801, 8801, 8802, 8802)");
    }

    private boolean entryCanceled() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("SELECT canceled_at IS NOT NULL FROM contest_entry WHERE contest_entry_id = 8801", Boolean.class);
    }

    @Test
    void 출품_중인_작품은_수정할_수_없다() {
        enterContest();

        assertThatThrownBy(() -> portfolioSubmitService.edit(OTHER, OTHERS, form(), null))
                .hasFieldOrPropertyWithValue("errorCode", PortfolioErrorCode.CONTEST_ENTERED);
        assertThat(row(OTHERS).get("title")).isEqualTo("남의 작품");
    }

    @Test
    void 출품작을_비공개로_바꾸면_출품을_취소하고_공개로_바꾸면_그대로다() {
        enterContest();
        portfolioManageService.changeVisibility(OTHER, OTHERS, true);
        assertThat(entryCanceled()).isFalse();

        portfolioManageService.changeVisibility(OTHER, OTHERS, false);
        assertThat(entryCanceled()).isTrue();
    }

    @Test
    void 출품작을_삭제하면_출품을_취소한다() {
        enterContest();
        portfolioManageService.delete(OTHER, OTHERS);
        assertThat(entryCanceled()).isTrue();
    }

    @Test
    void 회원_탈퇴로_포트폴리오를_모두_지우면_출품도_취소한다() {
        enterContest();
        portfolioContractService.deleteAllOf(OTHER);
        assertThat(row(OTHERS).get("deleted_at")).isNotNull();
        assertThat(entryCanceled()).isTrue();
    }

    private PortfolioSubmitForm form() {
        PortfolioSubmitForm form = new PortfolioSubmitForm();
        form.setTitle("고친 제목");
        form.setResultUrl("https://github.com/a/app2");
        form.setSummary("고친 요약");
        form.setRetrospective("다시 배운 점을 적습니다. ".repeat(10));
        form.setAiTools("Claude Code");
        form.setIsPublic(true);
        return form;
    }
}

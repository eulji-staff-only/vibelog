package com.vibelog.domain.admin.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 기본 대시보드 (A-01, API-ADM-01, FR-ADM-001, FR-ADM-002, #120). 넣은 데이터는 테스트가 끝나면 되돌린다 (@Transactional).
 *
 * 데이터(기간 2026-01-01~2026-01-31 기준):
 *   트랙A(9401) - 코스A1(9401): 1/3·1/15 시작·완료 2건(완료), 1/31 23시 시작 1건(미완료, 종료일 늦은 시각까지 포함되는지 확인용) = 3건 중 2건 완료(67%)
 *                              + 2/10 시작 1건(기간 밖, 미완료) → 기간 전체로 보면 4건 중 2건 완료(50%)
 *                - 코스A2(9402): 1/12 시작 1건(미완료) = 1건 중 0건 완료(0%)
 *                => 기간 필터: 수강 4건·완료율 50% / 기간 전체: 수강 5건·완료율 40%
 *   트랙B(9402) - 코스B1(9403): 1/2·1/28 시작 2건(모두 완료) = 수강 2건·완료율 100%
 *   회원 가입: 9401(1/5)·9402(1/20) 기간 안, 9403(2025-12-31)·9404(2026-02-01) 기간 밖 → 기간 가입자 수 2명
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminDashboardControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @WithMockUser(roles = "LEARNER")
    void 학습자는_들어올_수_없다() throws Exception {
        mockMvc.perform(get("/admin")).andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 기간을_지정하면_그_기간의_가입자_수와_트랙_코스별_수강_수_완료율을_보여준다() throws Exception {
        seedData();

        mockMvc.perform(get("/admin").param("from", "2026-01-01").param("to", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("2명"),
                        containsString("트랙A"),
                        containsString("수강 4건 · 완료율 50%"),
                        containsString("코스A1"),
                        containsString("67%"),
                        containsString("코스A2"),
                        containsString("트랙B"),
                        containsString("수강 2건 · 완료율 100%"),
                        containsString("코스B1"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 기간_파라미터가_없으면_전체_기간으로_본다() throws Exception {
        seedData();

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("수강 5건 · 완료율 40%"),
                        not(containsString("수강 4건 · 완료율 50%")))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 시작일이_종료일보다_늦으면_잘못된_요청이다() throws Exception {
        mockMvc.perform(get("/admin").param("from", "2026-02-01").param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("시작일이 종료일보다 늦습니다")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 진단_비율과_활동_건수를_기간에_맞춰_보여준다() throws Exception {
        seedActivityData();

        mockMvc.perform(get("/admin").param("from", "2020-05-01").param("to", "2020-05-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("진단 완료 3건 (75%) · 건너뜀 1건 (25%)"),
                        containsString("포트폴리오 제출"),
                        containsString("게시글"),
                        containsString("동료 리뷰"),
                        containsString("쪽지"),
                        containsString("신고"),
                        containsString("콘테스트 참여"),
                        containsString("2건"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 진단_결과가_없는_기간은_비율이_0퍼센트로_나온다() throws Exception {
        mockMvc.perform(get("/admin").param("from", "2019-01-01").param("to", "2019-01-31"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("진단 완료 0건 (0%) · 건너뜀 0건 (0%)")));
    }

    private void seedData() {
        memberOf(9401, "dash-a1@vibelog.test", "대시A1", "2026-01-05 00:00:00");
        memberOf(9402, "dash-a2@vibelog.test", "대시A2", "2026-01-20 00:00:00");
        memberOf(9403, "dash-a3@vibelog.test", "대시A3", "2025-12-31 23:59:59");
        memberOf(9404, "dash-a4@vibelog.test", "대시A4", "2026-02-01 00:00:01");
        memberOf(9405, "dash-a5@vibelog.test", "대시A5", "2025-06-01 00:00:00");
        memberOf(9406, "dash-a6@vibelog.test", "대시A6", "2025-06-01 00:00:00");
        memberOf(9407, "dash-a7@vibelog.test", "대시A7", "2025-06-01 00:00:00");
        memberOf(9408, "dash-a8@vibelog.test", "대시A8", "2025-06-01 00:00:00");
        memberOf(9409, "dash-a9@vibelog.test", "대시A9", "2025-06-01 00:00:00");

        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES "
                + "(9401, '트랙A', '대상', '설명', 9401), (9402, '트랙B', '대상', '설명', 9402)");
        jdbcTemplate.update("INSERT INTO course_category (course_category_id, name, sort_order) VALUES (9401, '카테고리', 9401)");
        jdbcTemplate.update("INSERT INTO course (course_id, track_id, course_category_id, title, summary, description, target, goal, plan_days) VALUES "
                + "(9401, 9401, 9401, '코스A1', '요약', '설명', '대상', '목표', 7), "
                + "(9402, 9401, 9401, '코스A2', '요약', '설명', '대상', '목표', 7), "
                + "(9403, 9402, 9401, '코스B1', '요약', '설명', '대상', '목표', 7)");

        // 코스A1: 기간 안 2건 완료 + 1건 미완료, 기간 밖(2월) 1건 미완료
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at, completed_at) VALUES "
                + "(9401, 9401, '2026-01-03 10:00:00', '2026-01-10 10:00:00'), "
                + "(9402, 9401, '2026-01-15 10:00:00', '2026-01-20 10:00:00'), "
                + "(9405, 9401, '2026-01-31 23:00:00', NULL), "
                + "(9409, 9401, '2026-02-10 10:00:00', NULL)");
        // 코스A2: 기간 안 1건 미완료
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at, completed_at) VALUES "
                + "(9406, 9402, '2026-01-12 10:00:00', NULL)");
        // 코스B1: 기간 안 2건 모두 완료
        jdbcTemplate.update("INSERT INTO enrollment (member_id, course_id, started_at, completed_at) VALUES "
                + "(9407, 9403, '2026-01-02 10:00:00', '2026-01-05 10:00:00'), "
                + "(9408, 9403, '2026-01-28 10:00:00', '2026-01-30 10:00:00')");
    }

    // 2020-05: 진단 완료 3·건너뜀 1, 게시글 2(+삭제 표시 1은 빠진다)
    private void seedActivityData() {
        memberOf(9411, "dash-b1@vibelog.test", "활동1", "2020-01-01 00:00:00");
        memberOf(9412, "dash-b2@vibelog.test", "활동2", "2020-01-01 00:00:00");
        memberOf(9413, "dash-b3@vibelog.test", "활동3", "2020-01-01 00:00:00");
        memberOf(9414, "dash-b4@vibelog.test", "활동4", "2020-01-01 00:00:00");
        jdbcTemplate.update("INSERT INTO track (track_id, name, target, description, sort_order) VALUES (9411, '활동트랙', '대상', '설명', 9411)");
        jdbcTemplate.update("INSERT INTO diagnosis_result (member_id, recommended_track_id, status, created_at) VALUES "
                + "(9411, 9411, 'COMPLETED', '2020-05-02 10:00:00'), (9412, 9411, 'COMPLETED', '2020-05-03 10:00:00'), "
                + "(9413, 9411, 'COMPLETED', '2020-05-04 10:00:00'), (9414, NULL, 'SKIPPED', '2020-05-05 10:00:00')");
        jdbcTemplate.update("INSERT INTO post (post_id, member_id, category, title, body, created_at, deleted_at) VALUES "
                + "(9411, 9411, 'FREE', '글1', '본문', '2020-05-02 10:00:00', NULL), (9412, 9412, 'FREE', '글2', '본문', '2020-05-03 10:00:00', NULL), "
                + "(9413, 9411, 'FREE', '삭제글', '본문', '2020-05-04 10:00:00', '2020-05-05 10:00:00')");
    }

    private void memberOf(long memberId, String email, String nickname, String createdAt) {
        jdbcTemplate.update("INSERT INTO member (member_id, email, password_hash, nickname, created_at) VALUES (?, ?, 'x', ?, ?)",
                memberId, email, nickname, createdAt);
    }
}

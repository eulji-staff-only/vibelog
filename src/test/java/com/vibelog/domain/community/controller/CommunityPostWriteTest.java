package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/*
 * 게시글 작성·수정·삭제 (API-CMT-03~07, #93). 넣은 회원·글은 테스트가 끝나면 되돌린다(@Transactional).
 * 올린 사진 파일은 build/test-uploads 에 저장한다 (빌드 결과물 폴더라 커밋되지 않고 clean 하면 지워진다).
 */
@SpringBootTest(properties = "vibelog.upload.dir=build/test-uploads")
@AutoConfigureMockMvc
@Transactional
class CommunityPostWriteTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13};

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberLoginService memberLoginService;

    /* 테스트 전체가 한 트랜잭션이라, 변경 감지로 바뀐 값은 flush 해야 JdbcTemplate 으로 읽힌다 */
    @Autowired
    EntityManager entityManager;

    private String verified;

    @BeforeEach
    void 인증한_회원을_넣는다() {
        verified = member(true);
    }

    @Test
    void 인증한_회원이_사진을_붙여_글을_쓰면_상세로_간다() throws Exception {
        String location = write(as(verified), "FREE", "첫 글", "본문입니다", null, image("a.png"), image("b.png"), empty())
                .andExpect(redirectedUrlPattern("/community/*"))
                .andReturn().getResponse().getRedirectedUrl();
        long postId = Long.parseLong(location.substring("/community/".length()));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT title, body, work_url FROM post WHERE post_id = ?", postId);
        assertThat(row).containsEntry("title", "첫 글").containsEntry("body", "본문입니다");
        assertThat(jdbcTemplate.queryForList("SELECT sort_order FROM post_image WHERE post_id = ? ORDER BY sort_order", Integer.class, postId))
                .containsExactly(1, 2);
    }

    @Test
    void 미인증_회원은_안내가_보이고_등록할_수_없다() throws Exception {
        String unverified = member(false);

        mockMvc.perform(get("/community/write").with(as(unverified)))
                .andExpect(content().string(allOf(containsString("이메일 인증 후 작성할 수 있습니다"), containsString("disabled"))));
        write(as(unverified), "FREE", "미인증 글", "본문", null)
                .andExpect(view().name("community/write"))
                .andExpect(content().string(containsString("이메일 인증을 마치면 글을 쓸 수 있습니다.")));
        assertThat(postCount("미인증 글")).isZero();
    }

    @Test
    void 작품_공유_글에_링크가_없으면_같은_폼에_오류와_입력값을_보여준다() throws Exception {
        write(as(verified), "SHOWCASE", "링크 없는 작품", "본문", "")
                .andExpect(view().name("community/write"))
                .andExpect(model().attributeHasFieldErrorCode("postWriteRequest", "workUrl", "SHOWCASE_LINK_REQUIRED"))
                .andExpect(content().string(containsString("value=\"링크 없는 작품\"")));
        write(as(verified), "SHOWCASE", "이상한 링크", "본문", "javascript:alert(1)")
                .andExpect(model().attributeHasFieldErrors("postWriteRequest", "workUrlValid"));
        assertThat(postCount("링크 없는 작품") + postCount("이상한 링크")).isZero();
    }

    @Test
    void 작품_공유가_아닌_글은_숨겨진_작품_칸을_저장하지_않는다() throws Exception {
        write(as(verified), "QNA", "질문 글", "본문", "not a url").andExpect(redirectedUrlPattern("/community/*"));

        assertThat(jdbcTemplate.queryForObject("SELECT work_url FROM post WHERE title = '질문 글'", String.class)).isNull();
    }

    @Test
    void 제목_본문_길이를_넘거나_비우면_저장하지_않는다() throws Exception {
        write(as(verified), "FREE", "가".repeat(101), "본문", null)
                .andExpect(model().attributeHasFieldErrors("postWriteRequest", "title"));
        write(as(verified), "FREE", "본문 없는 글", " ", null)
                .andExpect(model().attributeHasFieldErrors("postWriteRequest", "body"));
        write(as(verified), "FREE", "긴 본문", "가".repeat(5001), null)
                .andExpect(model().attributeHasFieldErrors("postWriteRequest", "body"));
        assertThat(postCount("본문 없는 글") + postCount("긴 본문")).isZero();
    }

    @Test
    void 사진은_3장까지_금칙어가_있으면_막는다() throws Exception {
        write(as(verified), "FREE", "사진 많은 글", "본문", null, image("1.png"), image("2.png"), image("3.png"), image("4.png"))
                .andExpect(model().attributeHasFieldErrorCode("postWriteRequest", "images", "INVALID_FILE"));
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES ('글쓰기금칙어')");
        write(as(verified), "FREE", "금칙어 글", "본문에 글쓰기금칙어 있음", null)
                .andExpect(content().string(containsString("사용할 수 없는 단어가 들어 있습니다.")));
        assertThat(postCount("사진 많은 글") + postCount("금칙어 글")).isZero();
    }

    @Test
    void 남의_글은_수정하거나_삭제할_수_없다() throws Exception {
        long postId = postOf(verified, "남의 글");
        RequestPostProcessor other = as(member(true));

        mockMvc.perform(get("/community/" + postId + "/edit").with(other)).andExpect(status().isForbidden());
        write(multipart("/community/" + postId + "/edit"), other, "FREE", "바꾼 제목", "본문", null)
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/community/" + postId + "/delete").with(other).with(csrf())).andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForMap("SELECT title, deleted_at FROM post WHERE post_id = ?", postId))
                .containsEntry("title", "남의 글").containsEntry("deleted_at", null);
    }

    @Test
    void 수정하면_고른_사진만_남기고_새_사진을_뒤에_붙인다() throws Exception {
        long postId = postOf(verified, "고칠 글");
        jdbcTemplate.update("INSERT INTO post_image (post_id, image_url, sort_order) VALUES (?, '/uploads/keep.png', 1), (?, '/uploads/drop.png', 2)", postId, postId);
        long keepId = jdbcTemplate.queryForObject("SELECT post_image_id FROM post_image WHERE post_id = ? AND sort_order = 1", Long.class, postId);

        mockMvc.perform(get("/community/" + postId + "/edit").with(as(verified)))
                .andExpect(content().string(allOf(containsString("value=\"고칠 글\""), containsString("/uploads/drop.png"))));
        MockMultipartHttpServletRequestBuilder edit = multipart("/community/" + postId + "/edit");
        edit.param("keepPostImageIds", String.valueOf(keepId));
        write(edit, as(verified), "FREE", "고친 글", "고친 본문", null, image("new.png"))
                .andExpect(redirectedUrl("/community/" + postId));
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT title FROM post WHERE post_id = ?", String.class, postId)).isEqualTo("고친 글");
        List<Map<String, Object>> images = jdbcTemplate.queryForList(
                "SELECT image_url, sort_order FROM post_image WHERE post_id = ? ORDER BY sort_order", postId);
        assertThat(images).hasSize(2);
        assertThat(images.get(0)).containsEntry("image_url", "/uploads/keep.png");
        assertThat((String) images.get(1).get("image_url")).startsWith("/uploads/").endsWith(".png");
        assertThat(images.get(1)).containsEntry("sort_order", 2);
    }

    @Test
    void 삭제하면_목록에서_빠지고_DB에는_남는다() throws Exception {
        long postId = postOf(verified, "지울 글");

        mockMvc.perform(post("/community/" + postId + "/delete").with(as(verified)).with(csrf()))
                .andExpect(redirectedUrl("/community"))
                .andExpect(flash().attribute("toast", "게시글을 삭제했습니다"));
        entityManager.flush();

        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at IS NOT NULL FROM post WHERE post_id = ?", Boolean.class, postId)).isTrue();
        mockMvc.perform(get("/community/" + postId)).andExpect(status().isNotFound());
    }

    private ResultActions write(RequestPostProcessor member, String category, String title, String body, String workUrl,
                                MockMultipartFile... images) throws Exception {
        return write(multipart("/community"), member, category, title, body, workUrl, images);
    }

    private ResultActions write(MockMultipartHttpServletRequestBuilder request, RequestPostProcessor member, String category,
                                String title, String body, String workUrl, MockMultipartFile... images) throws Exception {
        for (MockMultipartFile image : images) {
            request.file(image);
        }
        request.param("category", category).param("title", title).param("body", body);
        if (workUrl != null) {
            request.param("workUrl", workUrl);
        }
        return mockMvc.perform(request.with(member).with(csrf()));
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }

    private String member(boolean emailVerified) {
        String email = "writer-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname, email_verified_at) VALUES (?, 'x', '글쓴이', ?)",
                email, emailVerified ? LocalDateTime.now() : null);
        return email;
    }

    private long postOf(String email, String title) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body) "
                + "SELECT member_id, 'FREE', ?, '본문' FROM member WHERE email = ?", title, email);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int postCount(String title) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM post WHERE title = ?", Integer.class, title);
    }

    private static MockMultipartFile image(String name) {
        return new MockMultipartFile("images", name, "image/png", PNG);
    }

    private static MockMultipartFile empty() {
        return new MockMultipartFile("images", "", "application/octet-stream", new byte[0]);
    }
}

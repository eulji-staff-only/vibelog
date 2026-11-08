package com.vibelog.domain.community.controller;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import com.vibelog.domain.community.model.dto.PostListItem;
import com.vibelog.domain.community.model.entity.PostCategory;
import com.vibelog.domain.community.model.service.PostService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 게시판 목록·게시글 상세 (API-CMT-01·02, #92). 넣은 회원·글·댓글은 테스트가 끝나면 되돌린다(@Transactional).
 * 로컬 DB 에 다른 글이 있어도 섞이지 않게, 테스트마다 다른 제목 머리말(prefix)로 넣고 그 말로 검색한다.
 * 쿼리 수를 세려고 Hibernate 통계를 켠다 (이 테스트 클래스만).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class CommunityPostReadTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 9, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PostService postService;

    @Autowired
    MemberLoginService memberLoginService;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    private final String prefix = "읽기테스트" + UUID.randomUUID().toString().substring(0, 8);

    @Test
    void 목록은_삭제한_글을_빼고_최신순으로_작성자와_댓글_수를_보여준다() {
        long writer = member("글쓴이");
        long oldPost = post(writer, "FREE", prefix + " 먼저 쓴 글", BASE, null, null);
        post(writer, "QNA", prefix + " 나중에 쓴 글", BASE.plusHours(1), null, null);
        post(writer, "FREE", prefix + " 삭제한 글", BASE.plusHours(2), null, BASE.plusHours(3));
        comment(oldPost, writer, null, "댓글", BASE, BASE, null);
        comment(oldPost, writer, null, "지운 댓글", BASE, BASE, BASE.plusMinutes(1));

        Page<PostListItem> posts = postService.getPosts(null, prefix, 1);

        assertThat(posts.getContent()).extracting(PostListItem::title)
                .containsExactly(prefix + " 나중에 쓴 글", prefix + " 먼저 쓴 글");
        assertThat(posts.getContent()).extracting(PostListItem::nickname).containsOnly("글쓴이");
        assertThat(posts.getContent()).extracting(PostListItem::commentCount).containsExactly(0L, 1L);
    }

    @Test
    void 카테고리와_제목으로_거르고_20개씩_나눈다() {
        long writer = member("글쓴이");
        for (int i = 0; i < 21; i++) {
            post(writer, "SHOWCASE", prefix + " 작품 " + i, BASE.plusMinutes(i), "https://example.com", null);
        }
        post(writer, "FREE", prefix + " 자유 글", BASE.plusDays(1), null, null);

        assertThat(postService.getPosts(PostCategory.SHOWCASE, prefix, 1).getContent()).hasSize(20);
        assertThat(postService.getPosts(PostCategory.SHOWCASE, prefix, 2).getContent()).hasSize(1);
        assertThat(postService.getPosts(null, prefix + " 자유", 1).getContent()).extracting(PostListItem::title)
                .containsExactly(prefix + " 자유 글");
        assertThat(postService.getPosts(null, prefix, 0).getNumber()).as("0 이하 페이지는 첫 페이지").isZero();
    }

    @Test
    void 목록_20개를_읽어도_글마다_쿼리가_나가지_않는다() {
        for (int i = 0; i < 20; i++) {
            long writer = member("작성자" + i);
            long postId = post(writer, "FREE", prefix + " 글 " + i, BASE.plusMinutes(i), null, null);
            comment(postId, writer, null, "댓글", BASE, BASE, null);
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<PostListItem> posts = postService.getPosts(null, prefix, 1);

        assertThat(posts.getContent()).hasSize(20).allSatisfy(item -> assertThat(item.nickname()).startsWith("작성자"));
        // 목록 + 전체 개수 + 댓글 수 + 닉네임
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @Test
    void 목록_화면은_비회원에게_안내와_글을_보여주고_결과가_없으면_안내한다() throws Exception {
        long writer = member("글쓴이");
        post(writer, "SHOWCASE", prefix + " 화면에 보일 글", BASE, "https://example.com", null);

        mockMvc.perform(get("/community").param("q", prefix))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("게시글은 누구나 볼 수 있습니다"),
                        containsString(prefix + " 화면에 보일 글"),
                        containsString("작품 공유"))));
        mockMvc.perform(get("/community").param("q", prefix + "없는 제목"))
                .andExpect(content().string(containsString("조건에 맞는 글이 없습니다")));
    }

    @Test
    void 상세는_사진_작품_링크_댓글과_답글을_보여준다() throws Exception {
        long writer = member("글쓴이");
        long other = member("댓글러");
        long postId = post(writer, "SHOWCASE", prefix + " 상세 글", BASE, "https://example.com/app", null);
        jdbcTemplate.update("UPDATE post SET body = ?, tools = ? WHERE post_id = ?", "본문 첫 줄", "Claude, Cursor", postId);
        image(postId, "/uploads/b.png", 2);
        image(postId, "/uploads/a.png", 1);
        long edited = comment(postId, other, null, "수정한 댓글", BASE, BASE.plusMinutes(5), null);
        comment(postId, writer, edited, "답글입니다", BASE.plusMinutes(1), BASE.plusMinutes(1), null);
        comment(postId, other, edited, "지운 답글", BASE.plusMinutes(2), BASE.plusMinutes(2), BASE.plusMinutes(3));
        comment(postId, other, null, "지운 댓글", BASE.plusMinutes(4), BASE.plusMinutes(4), BASE.plusMinutes(6));

        String html = mockMvc.perform(get("/community/" + postId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(prefix + " 상세 글", "본문 첫 줄", "Claude, Cursor", "글쓴이",
                "href=\"https://example.com/app\"", "수정한 댓글", "수정됨", "답글입니다", "삭제된 댓글입니다", "댓글 2");
        assertThat(html.indexOf("/uploads/a.png")).as("사진은 순서대로").isLessThan(html.indexOf("/uploads/b.png"));
        assertThat(html).doesNotContain("지운 답글", "지운 댓글");
    }

    @Test
    void 수정_삭제_버튼은_내_글에만_보인다() throws Exception {
        String email = "post-owner-" + prefix.hashCode() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', '주인')", email);
        long owner = lastId();
        long postId = post(owner, "FREE", prefix + " 내 글", BASE, null, null);
        String editLink = "/community/" + postId + "/edit";

        mockMvc.perform(get("/community/" + postId).with(user(memberLoginService.loadUserByUsername(email))))
                .andExpect(content().string(containsString(editLink)));
        mockMvc.perform(get("/community/" + postId))
                .andExpect(content().string(not(containsString(editLink))));
    }

    @Test
    void 작품_링크가_http_주소가_아니면_링크로_만들지_않는다() throws Exception {
        long postId = post(member("글쓴이"), "SHOWCASE", prefix + " 이상한 링크", BASE, "javascript:alert(1)", null);

        mockMvc.perform(get("/community/" + postId))
                .andExpect(content().string(allOf(containsString("javascript:alert(1)"), not(containsString("href=\"javascript:")))));
    }

    @Test
    void 삭제했거나_없는_글은_찾을_수_없다고_안내한다() throws Exception {
        long postId = post(member("글쓴이"), "FREE", prefix + " 지운 글", BASE, null, BASE.plusHours(1));

        mockMvc.perform(get("/community/" + postId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("찾을 수 없습니다.")));
        mockMvc.perform(get("/community/999999999")).andExpect(status().isNotFound());
    }

    private long member(String nickname) {
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', ?)",
                "reader-" + UUID.randomUUID() + "@vibelog.test", nickname);
        return lastId();
    }

    private long post(long memberId, String category, String title, LocalDateTime createdAt, String workUrl, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO post (member_id, category, title, body, work_url, created_at, updated_at, deleted_at) "
                + "VALUES (?, ?, ?, '본문', ?, ?, ?, ?)", memberId, category, title, workUrl, createdAt, createdAt, deletedAt);
        return lastId();
    }

    private long comment(long postId, long memberId, Long parentId, String content,
                         LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        jdbcTemplate.update("INSERT INTO comment (post_id, member_id, parent_comment_id, content, created_at, updated_at, deleted_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", postId, memberId, parentId, content, createdAt, updatedAt, deletedAt);
        return lastId();
    }

    private void image(long postId, String url, int sortOrder) {
        jdbcTemplate.update("INSERT INTO post_image (post_id, image_url, sort_order) VALUES (?, ?, ?)", postId, url, sortOrder);
    }

    private long lastId() {
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}

package com.vibelog.global.file;

import com.vibelog.domain.auth.model.service.MemberLoginService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * 업로드 파일을 주소로 바로 열 때 비공개 포트폴리오 파일을 막는지 (#368, PL-SEC-002).
 * 파일은 build/test-uploads 에 만든다 (CommunityPostWriteTest 와 같은 폴더, 커밋되지 않음).
 */
@SpringBootTest(properties = "vibelog.upload.dir=build/test-uploads")
@AutoConfigureMockMvc
@Transactional
class UploadAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MemberLoginService memberLoginService;

    @Autowired
    EntityManager entityManager;

    String shot;
    String photo;
    String agentFile;
    long portfolioId;
    String owner;
    String other;

    @BeforeEach
    void setUp() throws Exception {
        shot = file("png");
        photo = file("png");
        agentFile = file("md");
        owner = member("주인");
        other = member("남");
        jdbcTemplate.update("INSERT INTO portfolio (member_id, portfolio_type, title, result_url, summary, retrospective, ai_tools, is_public, screenshot_url) "
                + "VALUES ((SELECT member_id FROM member WHERE email = ?), 'PERSONAL', '작품', 'https://example.com', '요약', '회고', 'Claude', FALSE, ?)", owner, shot);
        portfolioId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbcTemplate.update("UPDATE portfolio_assignment SET agent_file_url = ? WHERE portfolio_assignment_id = 1", agentFile);
    }

    @Test
    void 비공개_포트폴리오_스크린샷은_작성자와_운영자만_연다() throws Exception {
        mockMvc.perform(get(shot)).andExpect(status().isNotFound());
        mockMvc.perform(get(shot).with(as(other))).andExpect(status().isNotFound());
        mockMvc.perform(get(shot).with(as(owner))).andExpect(status().isOk());
        mockMvc.perform(get(shot).with(as("admin@vibelog.test"))).andExpect(status().isOk());
        // 인코딩한 주소(%61 = a)로 검사를 비켜 가지 못한다
        mockMvc.perform(get(URI.create(encodeFirstLetter(shot))).with(as(other))).andExpect(status().isNotFound());
    }

    @Test
    void 공개로_바꾸면_다른_회원도_열고_신고로_숨기거나_지우면_못_연다() throws Exception {
        jdbcTemplate.update("UPDATE portfolio SET is_public = TRUE WHERE portfolio_id = ?", portfolioId);
        mockMvc.perform(get(shot).with(as(other))).andExpect(status().isOk());

        jdbcTemplate.update("INSERT INTO report (reporter_member_id, target_type, target_id, target_member_id, reason, status) "
                + "SELECT member_id, 'PORTFOLIO', ?, (SELECT member_id FROM portfolio WHERE portfolio_id = ?), 'SPAM', 'HIDDEN' FROM member WHERE email = ?",
                portfolioId, portfolioId, other);
        mockMvc.perform(get(shot).with(as(other))).andExpect(status().isNotFound());

        jdbcTemplate.update("UPDATE portfolio SET deleted_at = NOW() WHERE portfolio_id = ?", portfolioId);
        entityManager.clear();   // 테스트는 요청들이 한 트랜잭션이라, 앞 요청이 읽은 포트폴리오가 1차 캐시에 남아 있다
        mockMvc.perform(get(shot).with(as(owner))).andExpect(status().isNotFound());
    }

    @Test
    void 평가_에이전트_설정_파일은_운영자만_주소로_열고_게시글_사진은_누구나_연다() throws Exception {
        mockMvc.perform(get(agentFile).with(as(owner))).andExpect(status().isNotFound());
        mockMvc.perform(get(agentFile).with(as("admin@vibelog.test"))).andExpect(status().isOk());
        mockMvc.perform(get(photo)).andExpect(status().isOk());
    }

    private String file(String extension) throws Exception {
        String name = UUID.randomUUID() + "." + extension;
        Path dir = Files.createDirectories(Path.of("build/test-uploads"));
        Files.writeString(dir.resolve(name), "x");
        return "/uploads/" + name;
    }

    private String member(String nickname) {
        String email = "upload-" + UUID.randomUUID() + "@vibelog.test";
        jdbcTemplate.update("INSERT INTO member (email, password_hash, nickname) VALUES (?, 'x', ?)", email, nickname);
        return email;
    }

    private RequestPostProcessor as(String email) {
        return user(memberLoginService.loadUserByUsername(email));
    }

    private static String encodeFirstLetter(String url) {
        int i = "/uploads/".length();
        return url.substring(0, i) + "%" + Integer.toHexString(url.charAt(i)) + url.substring(i + 1);
    }
}

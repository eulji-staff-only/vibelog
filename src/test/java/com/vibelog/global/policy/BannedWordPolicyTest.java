package com.vibelog.global.policy;

import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * 금칙어가 든 글은 BANNED_WORD 로 막고, 없으면 통과한다 (PL-SEC-003).
 * 테스트용 금칙어는 테스트마다 넣고 끝나면 되돌린다(@Transactional).
 * 로컬 MySQL 이 켜져 있고 sql/01-schema.sql 로 banned_word 테이블이 만들어져 있어야 통과한다.
 */
@SpringBootTest
@Transactional
class BannedWordPolicyTest {

    @Autowired
    BannedWordPolicy bannedWordPolicy;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void 테스트용_금칙어를_넣는다() {
        jdbcTemplate.update("INSERT INTO banned_word (word) VALUES (?), (?)", "테스트금칙어", "testbadword");
    }

    @Test
    void 글_안에_금칙어가_들어_있으면_막는다() {
        assertBanned("안녕하세요테스트금칙어입니다");
    }

    @Test
    void 대소문자만_다르게_써도_막는다() {
        assertBanned("this is TestBadWord");
    }

    @Test
    void 여러_칸_중_하나라도_걸리면_막는다() {
        assertBanned("정상 제목", null, "본문에 테스트금칙어");
    }

    @Test
    void 금칙어가_없거나_빈_칸이면_통과한다() {
        assertThatCode(() -> bannedWordPolicy.check("정상 제목", null, "정상 본문")).doesNotThrowAnyException();
    }

    private void assertBanned(String... texts) {
        assertThatThrownBy(() -> bannedWordPolicy.check(texts))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.BANNED_WORD);
    }
}

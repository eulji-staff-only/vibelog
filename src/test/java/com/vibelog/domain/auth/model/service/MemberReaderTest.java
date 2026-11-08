package com.vibelog.domain.auth.model.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/* sql/02-seed.sql 의 테스트 계정(1 학습자, 2 운영자)으로 Member 매핑과 닉네임 묶음 조회를 확인한다 */
@SpringBootTest
@Transactional
class MemberReaderTest {

    @Autowired
    MemberReader memberReader;

    @Test
    void ID_묶음으로_닉네임을_읽고_없는_ID는_빠진다() {
        assertThat(memberReader.getNicknames(List.of(1L, 2L, 999_999L)))
                .containsOnlyKeys(1L, 2L)
                .containsEntry(1L, "테스트학습자")
                .containsEntry(2L, "테스트운영자");
    }

    @Test
    void 빈_묶음이면_빈_결과() {
        assertThat(memberReader.getNicknames(List.of())).isEmpty();
    }

    @Test
    void 단건_조회로_닉네임을_읽고_없는_ID는_빈_문자열() {
        assertThat(memberReader.getNickname(1L)).isEqualTo("테스트학습자");
        assertThat(memberReader.getNickname(999_999L)).isEmpty();
    }
}

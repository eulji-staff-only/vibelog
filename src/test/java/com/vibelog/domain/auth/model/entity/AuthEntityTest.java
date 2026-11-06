package com.vibelog.domain.auth.model.entity;

import com.vibelog.domain.auth.model.dao.EmailVerificationRepository;
import com.vibelog.domain.auth.model.dao.SocialAccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/*
 * auth Entity 가 sql/01-schema.sql 테이블과 맞게 매핑됐는지, 저장하고 DB 에서 다시 읽어 확인한다.
 * 테스트가 끝나면 @Transactional 이 되돌리므로 DB 에 남지 않는다.
 */
@SpringBootTest
@Transactional
class AuthEntityTest {

    @Autowired
    SocialAccountRepository socialAccountRepository;

    @Autowired
    EmailVerificationRepository emailVerificationRepository;

    @Autowired
    EntityManager em;

    @Test
    void 소셜_연결_계정을_저장하고_읽는다() {
        Long id = socialAccountRepository.save(SocialAccount.create(1L, SocialProvider.GITHUB, "gh-test-1")).getSocialAccountId();
        em.flush();
        em.clear();     // 1차 캐시를 비워 DB 에서 다시 읽는다

        SocialAccount found = socialAccountRepository.findById(id).orElseThrow();

        assertThat(found.getMemberId()).isEqualTo(1L);
        assertThat(found.getProvider()).isEqualTo(SocialProvider.GITHUB);
        assertThat(found.getProviderUserId()).isEqualTo("gh-test-1");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void 이메일_인증_링크를_저장하고_읽는다() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24).withNano(0);
        Long id = emailVerificationRepository.save(EmailVerification.create(
                "learner@vibelog.test", "hash-test-1", VerificationPurpose.SIGNUP, expiresAt)).getEmailVerificationId();
        em.flush();
        em.clear();

        EmailVerification found = emailVerificationRepository.findById(id).orElseThrow();

        assertThat(found.getEmail()).isEqualTo("learner@vibelog.test");
        assertThat(found.getTokenHash()).isEqualTo("hash-test-1");
        assertThat(found.getPurpose()).isEqualTo(VerificationPurpose.SIGNUP);
        assertThat(found.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(found.getUsedAt()).isNull();
    }
}

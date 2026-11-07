package com.vibelog.domain.auth.model.policy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/* 구글 로그인은 인증된 이메일만 쓴다 (#91 리뷰). 인증 안 된 주소로 남의 이메일을 먼저 차지하지 못하게 */
class SocialLoginUserServiceTest {

    @Test
    void 구글은_email_verified_가_true_일_때만_이메일을_쓴다() {
        assertThat(SocialLoginUserService.googleEmail(Map.of("email", "a@vibelog.test", "email_verified", true)))
                .isEqualTo("a@vibelog.test");
        assertThat(SocialLoginUserService.googleEmail(Map.of("email", "a@vibelog.test", "email_verified", false))).isNull();
        assertThat(SocialLoginUserService.googleEmail(Map.of("email", "a@vibelog.test"))).isNull();
    }
}

package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.SocialAccount;
import com.vibelog.domain.auth.model.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    /* 가입 방식 표시 (마이페이지) */
    Optional<SocialAccount> findFirstByMemberId(Long memberId);

    /* 탈퇴하면 소셜 연결은 지운다 (DB 설계서 5-3 Hard Delete) */
    void deleteByMemberId(Long memberId);
}

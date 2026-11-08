package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.SocialAccount;
import com.vibelog.domain.auth.model.entity.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);
}

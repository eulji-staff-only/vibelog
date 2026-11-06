package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
}

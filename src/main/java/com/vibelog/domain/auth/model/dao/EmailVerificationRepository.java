package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
}

package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.EmailVerification;
import com.vibelog.domain.auth.model.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    /* 가장 최근에 보낸 링크 (다시 받기 1분 간격, PL-CST-002) */
    Optional<EmailVerification> findFirstByEmailAndPurposeOrderByEmailVerificationIdDesc(String email, VerificationPurpose purpose);

    /* 아직 쓰지 않은 링크. 새 링크를 보내면 모두 무효로 만든다 */
    List<EmailVerification> findByEmailAndPurposeAndUsedAtIsNull(String email, VerificationPurpose purpose);

    /* 탈퇴하면 그 이메일로 보낸 링크 기록도 지운다 (계정 정보 Hard Delete, PL-DAT-002) */
    void deleteByEmail(String email);
}

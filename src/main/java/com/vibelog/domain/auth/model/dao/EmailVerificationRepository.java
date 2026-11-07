package com.vibelog.domain.auth.model.dao;

import com.vibelog.domain.auth.model.entity.EmailVerification;
import com.vibelog.domain.auth.model.entity.VerificationPurpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    /* 링크를 쓸 때. 같은 링크로 동시에 두 번 써도 한 번만 통과하도록 행을 잠근다 (1회 사용, PL-USR-004) */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.tokenHash = :tokenHash")
    Optional<EmailVerification> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    /* 가장 최근에 보낸 링크 (다시 받기 1분 간격, PL-CST-002) */
    Optional<EmailVerification> findFirstByEmailAndPurposeOrderByEmailVerificationIdDesc(String email, VerificationPurpose purpose);

    /* 아직 쓰지 않은 링크. 새 링크를 보내면 모두 무효로 만든다 */
    List<EmailVerification> findByEmailAndPurposeAndUsedAtIsNull(String email, VerificationPurpose purpose);
}

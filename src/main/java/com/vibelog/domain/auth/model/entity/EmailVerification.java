package com.vibelog.domain.auth.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [EmailVerification]
 * 역할 : 이메일 인증·비밀번호 재설정 테이블(email_verification)과 매핑된다. 메일로 보낸 링크 1개 = 1행.
 * 책임 : 컬럼은 sql/01-schema.sql 의 email_verification 그대로 둔다.
 *        토큰 원문은 저장하지 않고 해시만 저장한다. 쓰거나 새 링크를 보내 무효로 만들면 used_at 을 채운다 (PL-USR-004).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long emailVerificationId;

    private String email;

    private String tokenHash;

    @Enumerated(EnumType.STRING)
    private VerificationPurpose purpose;

    private LocalDateTime expiresAt;    // 가입 인증 24시간, 비밀번호 재설정 30분 (PL-USR-004)

    private LocalDateTime usedAt;       // 사용하면 기록. 1회만 쓸 수 있다

    private EmailVerification(String email, String tokenHash, VerificationPurpose purpose, LocalDateTime expiresAt) {
        this.email = email;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(String email, String tokenHash, VerificationPurpose purpose,
                                           LocalDateTime expiresAt) {
        return new EmailVerification(email, tokenHash, purpose, expiresAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /* 링크를 썼거나, 새 링크를 보내 이전 링크를 무효로 만들 때 */
    public void use(LocalDateTime now) {
        this.usedAt = now;
    }
}

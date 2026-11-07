package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.EmailVerificationRepository;
import com.vibelog.domain.auth.model.entity.EmailVerification;
import com.vibelog.domain.auth.model.entity.VerificationPurpose;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/*
 * [EmailTokenService]
 * 역할 : 메일 링크에 넣는 토큰을 만들고 확인한다. 이메일 인증(#89)과 비밀번호 재설정(#90)이 같이 쓴다.
 * 책임 : - 토큰은 추측할 수 없는 무작위 값(32바이트)이고, DB 에는 SHA-256 해시만 저장한다 (email_verification.token_hash).
 *        - 새 링크를 만들면 같은 이메일·용도의 이전 링크는 무효로 만든다 (API-USR-10).
 *        - 없거나 이미 쓴 링크는 TOKEN_USED, 시간이 지난 링크는 TOKEN_EXPIRED (PL-USR-004). 두 경우 모두 "링크가 만료되었습니다."
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;

    /* 새 링크용 토큰을 돌려준다. 원문은 메일에만 들어가고 저장하지 않는다 */
    @Transactional
    public String issue(String email, VerificationPurpose purpose, Duration validFor) {
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findByEmailAndPurposeAndUsedAtIsNull(email, purpose).forEach(old -> old.use(now));
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        emailVerificationRepository.save(EmailVerification.create(email, hash(token), purpose, now.plus(validFor)));
        return token;
    }

    /*
     * 링크 없이 메일을 보낸 시각만 남긴다 (소셜 가입 회원에게 보내는 비밀번호 찾기 안내, #90).
     * 다시 보내기 간격(lastIssuedAt)을 같은 방식으로 세려고 한 행을 만들고 바로 사용 처리한다. 토큰은 메일에 넣지 않아 쓸 수 없다.
     */
    @Transactional
    public void recordSent(String email, VerificationPurpose purpose) {
        issue(email, purpose, Duration.ZERO);
        LocalDateTime now = LocalDateTime.now();
        emailVerificationRepository.findByEmailAndPurposeAndUsedAtIsNull(email, purpose).forEach(row -> row.use(now));
    }

    /* 마지막으로 링크를 보낸 시각 (다시 받기 간격 확인용) */
    public Optional<LocalDateTime> lastIssuedAt(String email, VerificationPurpose purpose) {
        return emailVerificationRepository.findFirstByEmailAndPurposeOrderByEmailVerificationIdDesc(email, purpose)
                .map(EmailVerification::getCreatedAt);
    }

    /* 쓸 수 있는 링크인지만 보고 이메일을 돌려준다 (재설정 화면을 열 때) */
    public String check(String token, VerificationPurpose purpose) {
        return find(token, purpose, LocalDateTime.now(), false).getEmail();
    }

    /* 링크를 쓴 것으로 기록하고 이메일을 돌려준다 */
    @Transactional
    public String use(String token, VerificationPurpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = find(token, purpose, now, true);
        verification.use(now);
        return verification.getEmail();
    }

    /* forUpdate 면 행을 잠그고 읽는다. 동시에 같은 링크를 써도 두 번째 요청은 첫 번째가 끝난 뒤 "사용함"을 본다 */
    private EmailVerification find(String token, VerificationPurpose purpose, LocalDateTime now, boolean forUpdate) {
        if (token == null) {
            throw new BusinessException(AuthErrorCode.TOKEN_USED);
        }
        EmailVerification verification = (forUpdate
                ? emailVerificationRepository.findByTokenHashForUpdate(hash(token))
                : emailVerificationRepository.findByTokenHash(hash(token)))
                .filter(found -> found.getPurpose() == purpose)
                .orElse(null);
        if (verification == null || verification.isUsed()) {
            throw new BusinessException(AuthErrorCode.TOKEN_USED);
        }
        if (verification.isExpired(now)) {
            throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
        }
        return verification;
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);   // 모든 JDK 에 SHA-256 이 있어 일어나지 않는다
        }
    }
}

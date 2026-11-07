package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import com.vibelog.domain.auth.model.entity.VerificationPurpose;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/*
 * [EmailVerifyService]
 * 역할 : 이메일 인증 (FR-USR-002, API-USR-03 의 인증 메일, API-USR-09·10).
 * 책임 : - 가입하면 인증 메일을 보낸다. 링크는 24시간 쓸 수 있다 (PL-USR-004).
 *        - 링크를 열면 member.email_verified_at 을 채운다. 인증 전에도 로그인·학습은 되고, 작성만 막힌다 (PL-USR-003).
 *        - 다시 받기는 1분 간격(RESEND_TOO_SOON, PL-CST-002), 이미 인증했으면 ALREADY_VERIFIED. 이전 링크는 무효가 된다.
 *        - 가입하지 않은 이메일로 다시 받기를 눌러도 같은 성공 응답을 준다 (가입 여부를 알리지 않는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerifyService {

    private static final Duration LINK_VALID = Duration.ofHours(24);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);

    private final MemberRepository memberRepository;
    private final EmailTokenService emailTokenService;
    private final AuthMailSender authMailSender;

    @Transactional
    public void sendSignupMail(String email) {
        String token = emailTokenService.issue(email, VerificationPurpose.SIGNUP, LINK_VALID);
        authMailSender.send(email, "[바이브로그] 이메일 인증을 마쳐 주세요", """
                바이브로그에 가입해 주셔서 감사합니다.
                아래 링크를 눌러 이메일 인증을 마쳐 주세요. 링크는 24시간 동안 쓸 수 있습니다.

                %s

                가입한 적이 없다면 이 메일은 무시해 주세요.
                """.formatted(authMailSender.link("/auth/verify?token=" + token)));
    }

    /* API-USR-09. 만료·사용한 링크면 TOKEN_EXPIRED·TOKEN_USED (화면 안 상태로 보여 준다) */
    @Transactional
    public void verify(String token) {
        String email = emailTokenService.use(token, VerificationPurpose.SIGNUP);
        Member member = memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.TOKEN_USED));   // 그 사이 탈퇴함
        member.verifyEmail(LocalDateTime.now());
    }

    /* API-USR-10 */
    @Transactional
    public void resend(String email) {
        Member member = memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE).orElse(null);
        if (member == null) {
            return;
        }
        if (member.isEmailVerified()) {
            throw new BusinessException(AuthErrorCode.ALREADY_VERIFIED);
        }
        LocalDateTime now = LocalDateTime.now();
        emailTokenService.lastIssuedAt(email, VerificationPurpose.SIGNUP)
                .filter(issuedAt -> now.isBefore(issuedAt.plus(RESEND_INTERVAL)))
                .ifPresent(issuedAt -> {
                    throw new BusinessException(AuthErrorCode.RESEND_TOO_SOON);
                });
        sendSignupMail(email);
    }
}

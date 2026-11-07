package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import com.vibelog.domain.auth.model.entity.VerificationPurpose;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/*
 * [PasswordResetService]
 * 역할 : 비밀번호 찾기·재설정 (FR-USR-005, API-USR-12~14).
 * 책임 : - 가입 여부는 알리지 않는다. 이메일 가입 회원에게만 재설정 링크(30분, 1회, PL-USR-004)를 보내고,
 *          소셜 가입 회원에게는 소셜 로그인 안내 메일을 보낸다. 가입하지 않은 이메일이면 아무것도 보내지 않는다.
 *        - 같은 이메일로 1분 안에 다시 요청하면 메일(재설정·안내 모두)을 다시 보내지 않는다 (요청 남용 방지, PL-CST-002 와 같은 간격). 화면은 똑같다.
 *        - 링크를 쓰면 새 비밀번호를 BCrypt 해시로 저장한다. 로그인 유지 쿠키는 비밀번호 해시로 서명되어 있어 함께 무효가 된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Duration LINK_VALID = Duration.ofMinutes(30);
    private static final Duration RESEND_INTERVAL = Duration.ofMinutes(1);

    private final MemberRepository memberRepository;
    private final EmailTokenService emailTokenService;
    private final AuthMailSender authMailSender;
    private final PasswordEncoder passwordEncoder;

    /* API-USR-12 */
    @Transactional
    public void sendResetMail(String email) {
        Member member = memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE).orElse(null);
        if (member == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        boolean tooSoon = emailTokenService.lastIssuedAt(email, VerificationPurpose.PASSWORD_RESET)
                .filter(issuedAt -> now.isBefore(issuedAt.plus(RESEND_INTERVAL)))
                .isPresent();
        if (tooSoon) {
            return;
        }
        if (!member.hasPassword()) {
            // 안내 메일도 1분 간격을 지키도록 보낸 시각만 남긴다 (링크가 없는 기록이라 쓸 수 없다)
            emailTokenService.recordSent(email, VerificationPurpose.PASSWORD_RESET);
            authMailSender.send(email, "[바이브로그] 로그인 방법 안내", """
                    이 이메일은 구글·깃허브 계정으로 가입되어 있어 비밀번호가 없습니다.
                    처음 가입한 방식(구글·깃허브)으로 로그인해 주세요.

                    %s

                    요청한 적이 없다면 이 메일은 무시해 주세요.
                    """.formatted(authMailSender.link("/login")));
            return;
        }
        String token = emailTokenService.issue(email, VerificationPurpose.PASSWORD_RESET, LINK_VALID);
        authMailSender.send(email, "[바이브로그] 비밀번호 재설정", """
                아래 링크를 눌러 새 비밀번호를 정해 주세요. 링크는 30분 동안 한 번만 쓸 수 있습니다.

                %s

                요청한 적이 없다면 이 메일은 무시해 주세요. 비밀번호는 바뀌지 않습니다.
                """.formatted(authMailSender.link("/password/reset?token=" + token)));
    }

    /* API-USR-13. 쓸 수 없는 링크면 TOKEN_EXPIRED·TOKEN_USED */
    public void checkLink(String token) {
        emailTokenService.check(token, VerificationPurpose.PASSWORD_RESET);
    }

    /* API-USR-14 */
    @Transactional
    public void reset(String token, String newPassword) {
        String email = emailTokenService.use(token, VerificationPurpose.PASSWORD_RESET);
        Member member = memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE)
                .filter(Member::hasPassword)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.TOKEN_USED));   // 그 사이 탈퇴함
        member.changePassword(passwordEncoder.encode(newPassword));
    }
}

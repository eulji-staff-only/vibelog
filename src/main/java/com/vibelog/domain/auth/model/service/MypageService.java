package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.EmailVerificationRepository;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dao.SocialAccountRepository;
import com.vibelog.domain.auth.model.dto.MypageInfo;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import com.vibelog.domain.auth.model.entity.SocialProvider;
import com.vibelog.domain.auth.model.policy.SuspensionPolicy;
import com.vibelog.domain.portfolio.model.policy.MemberPortfolioCleaner;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/*
 * [MypageService]
 * 역할 : 마이페이지의 내 정보·닉네임 저장·비밀번호 변경·회원 탈퇴 (FR-USR-007·008, API-USR-15~18).
 * 책임 : - 비밀번호 변경은 이메일 가입 회원만(소셜 가입 회원은 SOCIAL_ACCOUNT), 현재 비밀번호가 맞아야 한다(PASSWORD_MISMATCH).
 *        - 탈퇴는 DB 설계서 5-3 대로 한 트랜잭션에서: 계정 정보 비우기(Member.withdraw), 소셜 연결 삭제,
 *          포트폴리오 삭제 표시(계약 ⑩ MemberPortfolioCleaner), 그 이메일의 인증·재설정 링크 기록 삭제.
 *          게시글·댓글은 그대로 두고 작성자가 "탈퇴한 사용자"로 보인다 (PL-USR-009). 정지 중에는 탈퇴할 수 없다 (PL-USR-010).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MypageService {

    private final MemberRepository memberRepository;
    private final SuspensionPolicy suspensionPolicy;
    private final SocialAccountRepository socialAccountRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final MemberPortfolioCleaner memberPortfolioCleaner;
    private final PasswordEncoder passwordEncoder;
    private final BannedWordPolicy bannedWordPolicy;

    public MypageInfo getInfo(Long memberId) {
        Member member = getMember(memberId);
        String joinMethod = socialAccountRepository.findFirstByMemberId(memberId)
                .map(account -> account.getProvider() == SocialProvider.GOOGLE ? "구글" : "깃허브")
                .orElse("이메일");
        return new MypageInfo(member.getEmail(), member.getNickname(), joinMethod, member.hasPassword());
    }

    /*
     * API-USR-18. 지금 닉네임 그대로면 아무것도 하지 않는다.
     * ponytail: 가입과 같이 동시에 같은 닉네임으로 바꾸면 둘 다 통과할 수 있다 (member.nickname 에 UNIQUE 가 없다)
     */
    @Transactional
    public void changeNickname(Long memberId, String nickname) {
        Member member = getMember(memberId);
        if (nickname.equals(member.getNickname())) {
            return;
        }
        // 나는 빼고 본다. DB 비교는 대소문자를 가리지 않아 "Alice" → "alice" 가 나와 겹친다고 나온다
        if (memberRepository.existsByNicknameAndStatusAndMemberIdNot(nickname, MemberStatus.ACTIVE, memberId)) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
        bannedWordPolicy.check(nickname);
        member.changeNickname(nickname);
    }

    /* API-USR-17 */
    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = getMember(memberId);
        if (!member.hasPassword()) {
            throw new BusinessException(AuthErrorCode.SOCIAL_ACCOUNT);
        }
        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }
        member.changePassword(passwordEncoder.encode(newPassword));
    }

    /* API-USR-16. 로그아웃은 Controller 가 한다 */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = getMember(memberId);
        if (suspensionPolicy.isSuspended(memberId)) {
            throw new BusinessException(AuthErrorCode.SUSPENDED_CANNOT_WITHDRAW);
        }
        String email = member.getEmail();
        member.withdraw(LocalDateTime.now());
        socialAccountRepository.deleteByMemberId(memberId);
        emailVerificationRepository.deleteByEmail(email);
        memberPortfolioCleaner.deleteAllOf(memberId);
    }

    /* 이용 중인 회원만. 다른 기기에 남은 세션으로 탈퇴한 회원이 오면 USER_NOT_FOUND (다시 탈퇴·비밀번호 변경을 막는다) */
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));
    }
}

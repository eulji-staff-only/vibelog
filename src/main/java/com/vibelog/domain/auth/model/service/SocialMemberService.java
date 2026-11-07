package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dao.SocialAccountRepository;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import com.vibelog.domain.auth.model.entity.SocialAccount;
import com.vibelog.domain.auth.model.entity.SocialProvider;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/*
 * [SocialMemberService]
 * 역할 : 구글·깃허브 로그인으로 들어온 사람을 회원으로 찾거나 만든다 (FR-USR-003, API-USR-07).
 * 책임 : - 연결된 소셜 계정(provider + 고유 번호)이 있으면 그 회원으로 로그인한다.
 *        - 처음이면 회원과 소셜 연결을 만든다. 비밀번호는 없고 이메일은 인증된 것으로 본다 (PL-USR-003).
 *        - 같은 이메일이 다른 방식(이메일 가입·다른 소셜)으로 가입돼 있으면 SOCIAL_EMAIL_CONFLICT (PL-USR-005).
 *        - 닉네임은 소셜 이름에서 규칙(PL-USR-002: 2~12자 한글·영문·숫자)에 맞는 글자만 쓰고,
 *          겹치거나 쓸 수 없으면 뒤에 숫자 4자리를 붙인다. 이름이 짧거나 금칙어가 있으면 "회원"으로 시작한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialMemberService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final BannedWordPolicy bannedWordPolicy;

    @Transactional
    public LoginMember login(SocialProvider provider, String providerUserId, String email, String name) {
        Member linked = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .flatMap(account -> memberRepository.findById(account.getMemberId()))
                .filter(member -> member.getStatus() == MemberStatus.ACTIVE)
                .orElse(null);
        if (linked != null) {
            return LoginMember.social(linked, false);
        }
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_CONFLICT);
        }
        Member member = memberRepository.save(Member.createSocial(email, nicknameFrom(name), LocalDateTime.now()));
        socialAccountRepository.save(SocialAccount.create(member.getMemberId(), provider, providerUserId));
        return LoginMember.social(member, true);
    }

    private String nicknameFrom(String name) {
        String base = name == null ? "" : name.replaceAll("[^가-힣A-Za-z0-9]", "");
        base = base.substring(0, Math.min(base.length(), 8));
        if (base.length() < 2 || bannedWordPolicy.contains(base)) {
            base = "회원";
        }
        String nickname = base;
        while (memberRepository.existsByNicknameAndStatus(nickname, MemberStatus.ACTIVE)) {
            nickname = base + (1000 + RANDOM.nextInt(9000));
        }
        return nickname;
    }
}

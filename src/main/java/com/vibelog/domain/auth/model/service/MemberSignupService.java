package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.exception.AuthErrorCode;
import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.dto.SignupRequest;
import com.vibelog.domain.auth.model.entity.Member;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [MemberSignupService]
 * 역할 : 이메일 회원가입 (API-USR-03, FR-USR-001).
 * 책임 : - 이메일·닉네임 중복과 닉네임 금칙어(PL-USR-002, PL-SEC-003)를 검사하고, 비밀번호는 BCrypt 해시로 저장한다 (NFR-SEC-001).
 *        - 걸리면 BusinessException(DUPLICATE_EMAIL / DUPLICATE_NICKNAME / BANNED_WORD) 을 던진다.
 *        - 가입한 회원을 로그인 회원(LoginMember)으로 돌려준다. 로그인 상태로 만드는 일은 AuthController 가 한다.
 *        - 인증 메일 보내기는 #89 에서 붙인다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSignupService {

    private final MemberRepository memberRepository;
    private final BannedWordPolicy bannedWordPolicy;
    private final PasswordEncoder passwordEncoder;

    // ponytail: 같은 닉네임으로 동시에 가입하면 둘 다 통과할 수 있다 (member.nickname 에 UNIQUE 가 없다).
    //           문제가 되면 DB 팀에 UNIQUE 를 요청한다. 이메일은 uk_member_email 이 막는다
    @Transactional
    public LoginMember signup(SignupRequest request) {
        String email = request.getEmail().strip();
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
        }
        if (memberRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
        }
        bannedWordPolicy.check(request.getNickname());

        Member member = memberRepository.save(
                Member.create(email, passwordEncoder.encode(request.getPassword()), request.getNickname()));
        return LoginMember.from(member);
    }
}

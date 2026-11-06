package com.vibelog.domain.auth.model.service;

import com.vibelog.domain.auth.model.dao.MemberRepository;
import com.vibelog.domain.auth.model.dto.LoginMember;
import com.vibelog.domain.auth.model.entity.MemberStatus;
import com.vibelog.domain.auth.model.policy.LoginLockPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [MemberLoginService]
 * 역할 : 로그인할 때 Spring Security 가 이메일로 회원을 찾으러 부르는 곳 (UserDetailsService).
 * 책임 : - 이용 중(ACTIVE)인 회원만 돌려준다. 비밀번호 비교는 Spring Security 가 PasswordEncoder(BCrypt)로 한다.
 *        - 없으면 BusinessException 대신 UsernameNotFoundException 을 던진다 (Spring Security 약속).
 *          Security 가 이를 "비밀번호 틀림"과 같은 실패로 처리하므로 어느 값이 틀렸는지 드러나지 않는다 (PL-USR-007).
 *        - 5회 연속 실패로 잠긴 회원은 잠긴 상태로 돌려준다. 비밀번호가 맞아도 5분 동안 로그인할 수 없다 (PL-USR-007).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberLoginService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final LoginLockPolicy loginLockPolicy;

    @Override
    public UserDetails loadUserByUsername(String email) {
        return memberRepository.findByEmailAndStatus(email, MemberStatus.ACTIVE)
                .map(member -> LoginMember.from(member, loginLockPolicy.isLocked(member.getEmail())))
                .orElseThrow(() -> new UsernameNotFoundException("회원을 찾을 수 없습니다."));
    }
}

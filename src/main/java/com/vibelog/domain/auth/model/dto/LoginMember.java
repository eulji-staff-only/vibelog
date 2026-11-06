package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.Member;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/*
 * [LoginMember]
 * 역할 : 로그인한 회원. Spring Security 가 세션에 넣어 두고, 모든 영역의 Controller 가 꺼내 쓴다.
 * 책임 : - Controller 에서 @AuthenticationPrincipal LoginMember loginMember 로 받고,
 *          Service 에는 이 객체 대신 loginMember.getMemberId() 만 넘긴다. (README "로그인 약속")
 *        - 역할은 ROLE_LEARNER / ROLE_ADMIN 권한으로 바꿔 담는다 (SecurityConfig 의 hasRole("ADMIN")).
 *        - 비밀번호 해시는 로그인이 끝나면 Spring Security 가 지운다 (User.eraseCredentials).
 *        - locked 이면 Spring Security 가 비밀번호를 비교하기 전에 로그인을 막는다 (5회 연속 실패 잠금, PL-USR-007).
 */
@Getter
public class LoginMember extends User {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    private final Long memberId;

    private LoginMember(Long memberId, String email, String passwordHash, String role, boolean locked) {
        super(email, passwordHash, true, true, true, !locked, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.memberId = memberId;
    }

    public static LoginMember from(Member member, boolean locked) {
        return new LoginMember(member.getMemberId(), member.getEmail(), member.getPasswordHash(), member.getRole().name(), locked);
    }

    public boolean isAdmin() {
        return getAuthorities().contains(ADMIN);
    }
}

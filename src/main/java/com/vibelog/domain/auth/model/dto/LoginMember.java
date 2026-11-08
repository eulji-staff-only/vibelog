package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.Member;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

/*
 * [LoginMember]
 * 역할 : 로그인한 회원. Spring Security 가 세션에 넣어 두고, 모든 영역의 Controller 가 꺼내 쓴다.
 * 책임 : - Controller 에서 @AuthenticationPrincipal LoginMember loginMember 로 받고,
 *          Service 에는 이 객체 대신 loginMember.getMemberId() 만 넘긴다. (README "로그인 약속")
 *        - 역할은 ROLE_LEARNER / ROLE_ADMIN 권한으로 바꿔 담는다 (SecurityConfig 의 hasRole("ADMIN")).
 *        - 비밀번호 해시는 로그인이 끝나면 Spring Security 가 지운다 (User.eraseCredentials).
 *        - 구글·깃허브 로그인(#91)도 같은 LoginMember 를 쓴다 (OAuth2User). 그래서 어느 방식으로 로그인했든 꺼내는 법이 같다.
 *          소셜 가입 회원은 비밀번호가 없어 빈 값을 넣는다 (User 는 null 을 받지 않는다).
 */
@Getter
public class LoginMember extends User implements OAuth2User {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    private final Long memberId;

    private final boolean firstLogin;   // 소셜 로그인으로 방금 가입함 → 진단으로 보낸다 (API-USR-07)

    private LoginMember(Long memberId, String email, String passwordHash, String role, boolean firstLogin) {
        super(email, passwordHash == null ? "" : passwordHash, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.memberId = memberId;
        this.firstLogin = firstLogin;
    }

    public static LoginMember from(Member member) {
        return of(member, false);
    }

    /* 소셜 로그인 (#91). firstLogin 은 이번 로그인으로 계정을 만들었는지 */
    public static LoginMember social(Member member, boolean firstLogin) {
        return of(member, firstLogin);
    }

    private static LoginMember of(Member member, boolean firstLogin) {
        return new LoginMember(member.getMemberId(), member.getEmail(), member.getPasswordHash(), member.getRole().name(), firstLogin);
    }

    /* OAuth2User: 구글·깃허브가 준 값은 쓰지 않으므로 담지 않는다 */
    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public String getName() {
        return getUsername();
    }

    public boolean isAdmin() {
        return getAuthorities().contains(ADMIN);
    }
}

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
 * [Member]
 * 역할 : 회원 테이블(member)과 매핑된다. 학습자·운영자, 이메일 가입·소셜 가입 회원을 모두 담는다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 member 그대로 둔다.
 *        이메일 가입은 create 로 만든다. 상태 바꾸기 메서드는 탈퇴 기능 이슈에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    private String email;               // 탈퇴하면 NULL (ck_member_email_required)

    private String passwordHash;        // BCrypt 해시 (NFR-SEC-001). 소셜 가입 회원은 NULL

    private String nickname;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    private boolean isTest;             // 평균 점수 계산에서 빼는 테스트 계정 (PL-EXM-008)

    private LocalDateTime emailVerifiedAt;

    private LocalDateTime withdrawnAt;

    /* 이메일 가입 학습자 (FR-USR-001). 이메일 인증 전이다 (PL-USR-003) */
    public static Member create(String email, String passwordHash, String nickname) {
        Member member = new Member();
        member.email = email;
        member.passwordHash = passwordHash;
        member.nickname = nickname;
        member.role = MemberRole.LEARNER;
        member.status = MemberStatus.ACTIVE;
        return member;
    }

    /* 구글·깃허브로 처음 로그인한 학습자 (FR-USR-003). 비밀번호가 없고, 이메일은 인증된 것으로 본다 (PL-USR-003) */
    public static Member createSocial(String email, String nickname, LocalDateTime now) {
        Member member = create(email, null, nickname);
        member.emailVerifiedAt = now;
        return member;
    }

    /* 소셜 가입 회원은 비밀번호가 없다 (password_hash NULL) */
    public boolean hasPassword() {
        return passwordHash != null;
    }

    /* 비밀번호 재설정(#90)·변경(#116). BCrypt 해시를 받는다 */
    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    /* 인증 링크를 열면 (API-USR-09). 이미 인증했으면 처음 시각을 그대로 둔다 */
    public void verifyEmail(LocalDateTime now) {
        if (emailVerifiedAt == null) {
            this.emailVerifiedAt = now;
        }
    }
}

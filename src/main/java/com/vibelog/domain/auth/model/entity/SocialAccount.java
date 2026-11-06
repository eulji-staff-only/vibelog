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

/*
 * [SocialAccount]
 * 역할 : 소셜 연결 계정 테이블(social_account)과 매핑된다. 구글·깃허브 계정과 회원을 잇는다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 social_account 그대로 둔다.
 *        같은 소셜 계정(provider + provider_user_id)은 한 번만 연결된다 (uk_social_account_provider_user).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long socialAccountId;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private SocialProvider provider;

    private String providerUserId;      // 구글·깃허브가 주는 사용자 고유 번호

    private SocialAccount(Long memberId, SocialProvider provider, String providerUserId) {
        this.memberId = memberId;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public static SocialAccount create(Long memberId, SocialProvider provider, String providerUserId) {
        return new SocialAccount(memberId, provider, providerUserId);
    }
}

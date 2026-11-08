package com.vibelog.domain.community.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [Follow]
 * 역할 : 팔로우 테이블(follow)과 매핑된다. follower 가 following 을 팔로우한다 (FR-CMT-008).
 * 책임 : 컬럼은 sql/01-schema.sql 의 follow 그대로 둔다. 한 쌍은 한 번만(uk_follow_follower_following), 나는 팔로우할 수 없다(ck_follow_self).
 *        취소하면 행을 지운다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Follow extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followId;

    private Long followerMemberId;

    private Long followingMemberId;

    private Follow(Long followerMemberId, Long followingMemberId) {
        this.followerMemberId = followerMemberId;
        this.followingMemberId = followingMemberId;
    }

    public static Follow create(Long followerMemberId, Long followingMemberId) {
        return new Follow(followerMemberId, followingMemberId);
    }
}

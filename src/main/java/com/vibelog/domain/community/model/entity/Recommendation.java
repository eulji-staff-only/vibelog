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
 * [Recommendation]
 * 역할 : 작품 공유 글 추천 1건 (recommendation 테이블, FR-CMT-005).
 * 책임 : 1인 1글 1회는 uk_recommendation_member_post 가 막는다 (PL-CMT-001). 취소하면 행을 지운다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationId;

    private Long memberId;

    private Long postId;

    private Recommendation(Long memberId, Long postId) {
        this.memberId = memberId;
        this.postId = postId;
    }

    public static Recommendation create(Long memberId, Long postId) {
        return new Recommendation(memberId, postId);
    }
}

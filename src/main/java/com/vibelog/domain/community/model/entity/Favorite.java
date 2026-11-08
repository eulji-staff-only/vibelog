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
 * [Favorite]
 * 역할 : 즐겨찾기 테이블(favorite)과 매핑된다. 회원 한 명이 게시글 하나를 한 번만 즐겨찾기한다 (uk_favorite_member_post, FR-CMT-007).
 * 책임 : 컬럼은 sql/01-schema.sql 의 favorite 그대로 둔다. 취소하면 행을 지운다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long favoriteId;

    private Long memberId;

    private Long postId;

    private Favorite(Long memberId, Long postId) {
        this.memberId = memberId;
        this.postId = postId;
    }

    public static Favorite create(Long memberId, Long postId) {
        return new Favorite(memberId, postId);
    }
}

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
 * [PostImage]
 * 역할 : 게시글 사진 테이블(post_image)과 매핑된다. 글 하나에 최대 3장 (PL-CMT-009).
 * 책임 : 컬럼은 sql/01-schema.sql 의 post_image 그대로 둔다. sort_order 순서대로 보여 준다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postImageId;

    private Long postId;

    private String imageUrl;            // FileStorage 가 돌려준 /uploads/... 주소

    private int sortOrder;

    private PostImage(Long postId, String imageUrl, int sortOrder) {
        this.postId = postId;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    public static PostImage create(Long postId, String imageUrl, int sortOrder) {
        return new PostImage(postId, imageUrl, sortOrder);
    }
}

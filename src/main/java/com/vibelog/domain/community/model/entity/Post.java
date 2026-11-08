package com.vibelog.domain.community.model.entity;

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
 * [Post]
 * 역할 : 게시글 테이블(post)과 매핑된다.
 * 책임 : 컬럼은 sql/01-schema.sql 의 post 그대로 둔다. 작성자는 회원 ID(memberId)로만 참조한다.
 *        삭제하면 deleted_at 이 채워지고 목록·상세에서 빠진다. 만들기(create)·수정·삭제 메서드는 글쓰기 이슈(#93)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    private Long memberId;

    @Enumerated(EnumType.STRING)
    private PostCategory category;

    private String title;

    private String body;

    private String workUrl;             // 작품 공유 글만 (ck_post_showcase_work_url)

    private String tools;

    private LocalDateTime deletedAt;
}

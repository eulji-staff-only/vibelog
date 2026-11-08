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
 *        삭제하면 deleted_at 이 채워지고 목록·상세에서 빠진다. 행은 남는다 (PL-DAT-002).
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

    private Post(Long memberId, PostCategory category, String title, String body, String workUrl, String tools) {
        this.memberId = memberId;
        change(category, title, body, workUrl, tools);
    }

    /* 작품 링크·사용한 도구는 작품 공유 글만 둔다 (PL-CMT-008) */
    public static Post create(Long memberId, PostCategory category, String title, String body, String workUrl, String tools) {
        return new Post(memberId, category, title, body, workUrl, tools);
    }

    public void update(PostCategory category, String title, String body, String workUrl, String tools) {
        change(category, title, body, workUrl, tools);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isWrittenBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    private void change(PostCategory category, String title, String body, String workUrl, String tools) {
        boolean showcase = category == PostCategory.SHOWCASE;
        this.category = category;
        this.title = title;
        this.body = body;
        this.workUrl = showcase ? workUrl : null;
        this.tools = showcase ? tools : null;
    }
}

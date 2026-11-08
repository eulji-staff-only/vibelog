package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;

import java.time.LocalDateTime;

/* 게시판 목록 한 줄 (API-CMT-01): 카테고리, 제목, 작성자, 작성일, 댓글 수 */
public record PostListItem(Long postId, PostCategory category, String title, String nickname,
                           LocalDateTime createdAt, long commentCount) {

    public static PostListItem from(Post post, String nickname, long commentCount) {
        return new PostListItem(post.getPostId(), post.getCategory(), post.getTitle(), nickname,
                post.getCreatedAt(), commentCount);
    }
}

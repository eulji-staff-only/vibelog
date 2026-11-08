package com.vibelog.domain.community.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/*
 * [PostCategory]
 * 역할 : 게시판 카테고리 (post.category, ck_post_category). PL-CMT-008
 * 책임 : 화면에 보이는 이름(label)을 같이 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum PostCategory {
    FREE("자유게시판"),
    QNA("질문·답변"),
    SHOWCASE("작품 공유");

    private final String label;
}

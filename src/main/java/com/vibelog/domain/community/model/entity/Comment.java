package com.vibelog.domain.community.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [Comment]
 * 역할 : 댓글·답글 테이블(comment)과 매핑된다. 답글은 parent_comment_id 가 있는 댓글이다 (한 단계까지, PL-CMT-006).
 * 책임 : - 컬럼은 sql/01-schema.sql 의 comment 그대로 둔다.
 *        - 수정했으면 "수정됨", 삭제했으면 "삭제된 댓글입니다"로 보여 준다 (PL-CMT-006). 판단은 아래 메서드로 한다.
 *        - 만들기(create)·수정·삭제 메서드는 댓글 이슈(#94)에서 추가한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    private Long postId;

    private Long memberId;

    private Long parentCommentId;       // 댓글이면 NULL, 답글이면 부모 댓글 ID

    private String content;

    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /* 만들 때 created_at 과 updated_at 이 같게 들어가므로, 나중에 바뀌었으면 수정한 것이다 */
    public boolean isEdited() {
        return !isDeleted() && getUpdatedAt().isAfter(getCreatedAt());
    }
}

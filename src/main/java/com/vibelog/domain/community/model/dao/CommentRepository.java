package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /* 삭제된 댓글도 "삭제된 댓글입니다"로 보여 줘야 하므로 모두 읽는다 */
    List<Comment> findByPostIdOrderByCreatedAtAscCommentIdAsc(Long postId);

    @Query("select new com.vibelog.domain.community.model.dao.PostCommentCount(c.postId, count(c)) "
            + "from Comment c where c.postId in :postIds and c.deletedAt is null group by c.postId")
    List<PostCommentCount> countByPostIds(@Param("postIds") Collection<Long> postIds);
}

package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {

    List<ReviewComment> findByPeerReviewIdInOrderByReviewCommentIdAsc(Collection<Long> peerReviewIds);

    /* 리뷰를 고치거나 되살릴 때 코멘트를 새로 넣기 전에 지운다 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ReviewComment c where c.peerReviewId = :peerReviewId")
    void deleteByPeerReviewId(@Param("peerReviewId") Long peerReviewId);
}

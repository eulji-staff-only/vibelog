package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.PeerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {

    /* 지운 리뷰도 찾는다 (다시 쓰면 되살린다) */
    Optional<PeerReview> findByReviewRequestIdAndReviewerMemberId(Long reviewRequestId, Long reviewerMemberId);

    /* U-31 내가 쓴 리뷰, 최근순 */
    Page<PeerReview> findByReviewerMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPeerReviewIdDesc(Long reviewerMemberId, Pageable pageable);

    /* U-20 받은 리뷰, 오래된 순 */
    List<PeerReview> findByReviewRequestIdInAndDeletedAtIsNullOrderByCreatedAtAscPeerReviewIdAsc(Collection<Long> reviewRequestIds);

    /* U-31 카드의 받은 리뷰 수: [reviewRequestId, count] */
    @Query("select r.reviewRequestId, count(r) from PeerReview r where r.reviewRequestId in :ids and r.deletedAt is null"
            + " group by r.reviewRequestId")
    List<Object[]> countLiveByRequestIds(@Param("ids") Collection<Long> reviewRequestIds);
}

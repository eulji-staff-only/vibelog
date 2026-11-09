package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.dto.WeeklyRecommendCount;
import com.vibelog.domain.community.model.entity.Recommendation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findByMemberIdAndPostId(Long memberId, Long postId);

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    long countByPostId(Long postId);

    /*
     * 추천 랭킹 (PL-CMT-003·004): since 이후 받은 추천 수가 많은 작품 공유 글 순, 동점이면 그 기간 첫 추천이 빠른 글.
     * 지운 글은 뺀다. 개수는 pageable 로 자른다.
     */
    @Query("select new com.vibelog.domain.community.model.dto.WeeklyRecommendCount(r.postId, count(r), min(r.createdAt))"
            + " from Recommendation r join Post p on p.postId = r.postId"
            + " where r.createdAt >= :since and p.deletedAt is null"
            + " and p.category = com.vibelog.domain.community.model.entity.PostCategory.SHOWCASE"
            + " and p.memberId not in :hiddenMemberIds"
            + " group by r.postId order by count(r) desc, min(r.createdAt) asc, r.postId asc")
    List<WeeklyRecommendCount> findWeeklyCounts(@Param("since") LocalDateTime since,
                                                @Param("hiddenMemberIds") Collection<Long> hiddenMemberIds, Pageable pageable);
}

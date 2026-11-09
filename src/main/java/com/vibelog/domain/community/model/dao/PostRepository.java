package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByPostIdAndDeletedAtIsNull(Long postId);

    /*
     * 게시판 목록 (API-CMT-01). category·keyword 가 null 이면 그 조건은 빼고, 최신순 (PL-CMT-005).
     * hiddenMemberIds(영구 정지 회원, PL-RPT-006)의 글과 hiddenPostIds(신고 숨김, FR-RPT-003)는 뺀다.
     * 빈 묶음이면 IN () 이 되지 않게 부르는 쪽이 0 하나를 넣는다.
     */
    @Query("select p from Post p where p.deletedAt is null"
            + " and (:category is null or p.category = :category)"
            + " and (:keyword is null or p.title like concat('%', :keyword, '%'))"
            + " and p.memberId not in :hiddenMemberIds and p.postId not in :hiddenPostIds"
            + " order by p.createdAt desc, p.postId desc")
    Page<Post> search(@Param("category") PostCategory category, @Param("keyword") String keyword,
                      @Param("hiddenMemberIds") Collection<Long> hiddenMemberIds, @Param("hiddenPostIds") Collection<Long> hiddenPostIds,
                      Pageable pageable);

    /* 마이페이지 "작성한 글" (API-USR-15). 최신순 */
    Page<Post> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPostIdDesc(Long memberId, Pageable pageable);

    /* 공개 프로필 게시글 (API-CMT-14). 신고로 숨긴 글(FR-RPT-003)은 뺀다. 빈 묶음이면 부르는 쪽이 0 하나를 넣는다 */
    Page<Post> findByMemberIdAndDeletedAtIsNullAndPostIdNotInOrderByCreatedAtDescPostIdDesc(Long memberId, Collection<Long> hiddenPostIds,
                                                                                            Pageable pageable);

    /* 기간 내 게시글 수, 삭제 표시 제외 (created_at 기준). from·to 가 null 이면 그 쪽 제한 없음 (admin 확장 대시보드, FR-ADM-003) */
    @Query("select count(p) from Post p where p.deletedAt is null and (:from is null or p.createdAt >= :from)"
            + " and (:to is null or p.createdAt <= :to)")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}

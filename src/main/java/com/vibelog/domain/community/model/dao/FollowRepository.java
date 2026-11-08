package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerMemberIdAndFollowingMemberId(Long followerMemberId, Long followingMemberId);

    boolean existsByFollowerMemberIdAndFollowingMemberId(Long followerMemberId, Long followingMemberId);

    /* 팔로워 수 */
    long countByFollowingMemberId(Long followingMemberId);

    /* 팔로잉 수 */
    long countByFollowerMemberId(Long followerMemberId);

    /* 마이페이지 팔로우 탭: 내가 팔로우한 회원, 최근 팔로우 순 */
    Page<Follow> findByFollowerMemberIdOrderByCreatedAtDescFollowIdDesc(Long followerMemberId, Pageable pageable);
}

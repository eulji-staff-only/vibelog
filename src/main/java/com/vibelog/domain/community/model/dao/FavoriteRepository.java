package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.Favorite;
import com.vibelog.domain.community.model.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByMemberIdAndPostId(Long memberId, Long postId);

    boolean existsByMemberIdAndPostId(Long memberId, Long postId);

    /* 마이페이지 즐겨찾기 탭: 지운 글은 빼고, 즐겨찾기한 순서 최신순 */
    @Query(value = "select p from Favorite f join Post p on p.postId = f.postId"
            + " where f.memberId = :memberId and p.deletedAt is null order by f.createdAt desc, f.favoriteId desc",
            countQuery = "select count(f) from Favorite f join Post p on p.postId = f.postId"
                    + " where f.memberId = :memberId and p.deletedAt is null")
    Page<Post> findLivePostsFavoritedBy(@Param("memberId") Long memberId, Pageable pageable);
}

package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByPostIdAndDeletedAtIsNull(Long postId);

    /* 게시판 목록 (API-CMT-01). category·keyword 가 null 이면 그 조건은 빼고, 최신순 (PL-CMT-005) */
    @Query("select p from Post p where p.deletedAt is null"
            + " and (:category is null or p.category = :category)"
            + " and (:keyword is null or p.title like concat('%', :keyword, '%'))"
            + " order by p.createdAt desc, p.postId desc")
    Page<Post> search(@Param("category") PostCategory category, @Param("keyword") String keyword, Pageable pageable);
}

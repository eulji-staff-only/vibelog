package com.vibelog.domain.community.model.dao;

import com.vibelog.domain.community.model.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderBySortOrder(Long postId);
}

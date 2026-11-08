package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.community.model.entity.PostImage;

/* 수정 화면의 기존 사진 하나 (남길지 고른다) */
public record PostImageView(Long postImageId, String imageUrl) {

    public static PostImageView from(PostImage image) {
        return new PostImageView(image.getPostImageId(), image.getImageUrl());
    }
}

package com.vibelog.domain.community.model.service;

import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.FavoriteRepository;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.entity.Favorite;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [FavoriteService]
 * 역할 : 게시글 즐겨찾기·취소 (FR-CMT-007, API-CMT-13).
 * 책임 : - 같은 요청으로 켜고 끈다 (없으면 추가, 있으면 취소). 모든 카테고리 글을 즐겨찾기할 수 있다.
 *        - 없거나 지운 글은 POST_NOT_FOUND.
 *        - 추천과 달리 "작성"이 아니라 이메일 인증 없이 된다 (PL-USR-003).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PostRepository postRepository;

    /* 바꾼 뒤 즐겨찾기 상태를 돌려준다 */
    public boolean toggle(Long memberId, Long postId) {
        if (postRepository.findByPostIdAndDeletedAtIsNull(postId).isEmpty()) {
            throw new BusinessException(CommunityErrorCode.POST_NOT_FOUND);
        }
        return favoriteRepository.findByMemberIdAndPostId(memberId, postId)
                .map(favorite -> {
                    favoriteRepository.delete(favorite);
                    return false;
                })
                .orElseGet(() -> {
                    favoriteRepository.save(Favorite.create(memberId, postId));
                    return true;
                });
    }
}

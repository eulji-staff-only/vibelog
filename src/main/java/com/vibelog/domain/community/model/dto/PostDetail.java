package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;

import java.time.LocalDateTime;
import java.util.List;

/*
 * 게시글 상세 (API-CMT-02).
 * workUrl 은 http·https 주소일 때만 링크로 보여 준다(workUrlLinkable). "javascript:" 같은 주소가 링크가 되지 않게 한다.
 * authorProfileId 는 작성자 공개 프로필(U-29) 링크용. 탈퇴한 작성자면 null 이라 닉네임 글자만 보인다 (API-CMT-02 P2).
 * hiddenNotice 는 신고로 숨긴 내 글이라 숨김 안내를 붙인다 (다른 사람은 상세를 볼 수 없다, FR-RPT-003).
 */
public record PostDetail(Long postId, PostCategory category, String title, String nickname, LocalDateTime createdAt,
                         String body, List<String> imageUrls, String workUrl, boolean workUrlLinkable, String tools,
                         long commentCount, List<CommentView> comments, boolean mine, boolean favorited,
                         Long authorProfileId, boolean hiddenNotice) {

    public static PostDetail of(Post post, String nickname, List<String> imageUrls, long commentCount,
                                List<CommentView> comments, boolean mine, boolean favorited, boolean authorVisible,
                                boolean hiddenNotice) {
        String workUrl = post.getWorkUrl();
        boolean linkable = workUrl != null && (workUrl.startsWith("https://") || workUrl.startsWith("http://"));
        return new PostDetail(post.getPostId(), post.getCategory(), post.getTitle(), nickname, post.getCreatedAt(),
                post.getBody(), imageUrls, workUrl, linkable, post.getTools(), commentCount, comments, mine, favorited,
                authorVisible ? post.getMemberId() : null, hiddenNotice);
    }
}

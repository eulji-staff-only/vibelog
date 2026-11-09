package com.vibelog.domain.community.model.service;

import com.vibelog.domain.community.model.dao.CommentRepository;
import com.vibelog.domain.community.model.dao.FavoriteRepository;
import com.vibelog.domain.community.model.dao.PostCommentCount;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.dto.MemberCommentItem;
import com.vibelog.domain.community.model.dto.MemberPostItem;
import com.vibelog.domain.community.model.entity.Comment;
import com.vibelog.domain.community.model.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * [CommunityReader]
 * 역할 : 다른 영역이 커뮤니티 값(회원이 쓴 글·댓글 목록)을 읽어 가는 곳. 읽기 전용이다.
 * 책임 : - 다른 영역은 Post·Comment·Repository 를 import 하지 않고 이 클래스만 주입해 쓴다 (08 문서 규칙 1).
 *        - 마이페이지(API-USR-15)의 작성한 글·댓글 목록을 페이지로 준다. 지운 글·댓글은 빼고 최신순이다.
 *        - page 는 화면 그대로 1부터 센다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityReader {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final FavoriteRepository favoriteRepository;

    public Page<MemberPostItem> getPostsOf(Long memberId, int page) {
        return toItems(postRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPostIdDesc(
                memberId, PageRequest.of(Math.max(page, 1) - 1, PostService.PAGE_SIZE)));
    }

    /* 마이페이지 즐겨찾기 탭 (API-USR-15 P2, FR-CMT-007). 지운 글은 빼고 즐겨찾기한 순서 최신순 */
    public Page<MemberPostItem> getFavoritePostsOf(Long memberId, int page) {
        return toItems(favoriteRepository.findLivePostsFavoritedBy(
                memberId, PageRequest.of(Math.max(page, 1) - 1, PostService.PAGE_SIZE)));
    }

    private Page<MemberPostItem> toItems(Page<Post> posts) {
        List<Long> postIds = posts.map(Post::getPostId).toList();
        Map<Long, Long> commentCounts = postIds.isEmpty() ? Map.of() : commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostCommentCount::postId, PostCommentCount::count));
        return posts.map(post -> new MemberPostItem(post.getPostId(), post.getTitle(), post.getCategory().getLabel(),
                post.getCreatedAt(), commentCounts.getOrDefault(post.getPostId(), 0L)));
    }

    /* 알림 도착지 (message API-NTF-02). 지운 댓글도 행이 남아 글 ID 를 돌려준다. 행이 없으면 빈 값 */
    public Optional<Long> findPostIdOfComment(Long commentId) {
        return commentRepository.findById(commentId).map(Comment::getPostId);
    }

    public Page<MemberCommentItem> getCommentsOf(Long memberId, int page) {
        Page<Comment> comments = commentRepository.findLiveByMemberId(
                memberId, PageRequest.of(Math.max(page, 1) - 1, PostService.PAGE_SIZE));
        Map<Long, String> titles = postRepository.findAllById(comments.map(Comment::getPostId).toSet()).stream()
                .collect(Collectors.toMap(Post::getPostId, Post::getTitle, (a, b) -> a));
        return comments.map(comment -> new MemberCommentItem(comment.getPostId(), comment.getCommentId(),
                comment.getContent(), titles.get(comment.getPostId()), comment.getCreatedAt()));
    }
}

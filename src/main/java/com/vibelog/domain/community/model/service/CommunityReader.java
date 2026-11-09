package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.model.dao.CommentRepository;
import com.vibelog.domain.community.model.dao.FavoriteRepository;
import com.vibelog.domain.community.model.dao.FollowRepository;
import com.vibelog.domain.community.model.dao.PostCommentCount;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.dto.ReviewView;
import com.vibelog.domain.community.model.dto.MemberCommentItem;
import com.vibelog.domain.community.model.dto.MemberPostItem;
import com.vibelog.domain.community.model.dto.MemberSummary;
import com.vibelog.domain.community.model.entity.Comment;
import com.vibelog.domain.community.model.entity.Follow;
import com.vibelog.domain.community.model.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.Set;
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
    private final FollowRepository followRepository;
    private final MemberReader memberReader;
    private final PeerReviewService peerReviewService;

    public Page<MemberPostItem> getPostsOf(Long memberId, int page) {
        return toItems(postRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescPostIdDesc(
                memberId, PageRequest.of(Math.max(page, 1) - 1, PostService.PAGE_SIZE)));
    }

    /* 마이페이지 즐겨찾기 탭 (API-USR-15 P2, FR-CMT-007). 지운 글은 빼고 즐겨찾기한 순서 최신순 */
    public Page<MemberPostItem> getFavoritePostsOf(Long memberId, int page) {
        return toItems(favoriteRepository.findLivePostsFavoritedBy(
                memberId, PageRequest.of(Math.max(page, 1) - 1, PostService.PAGE_SIZE)));
    }

    /*
     * 마이페이지 팔로우 탭 (API-USR-15 P2, FR-CMT-008). 내가 팔로우한 회원, 최근 팔로우 순. 볼 수 없는 회원(탈퇴 등)은 뺀다.
     * 회원 상태는 auth 테이블이라 쿼리로 거를 수 없어서, 내 팔로우를 모두 읽고 거른 뒤 쪽을 나눈다 (쪽마다 20명·전체 수가 맞게).
     */
    // ponytail: 한 회원의 팔로우를 모두 읽는다. 팔로우가 수천 명 규모가 되면 MemberReader 에 볼 수 없는 회원 조회를 두고 쿼리로 나눈다
    public Page<MemberSummary> getFollowingOf(Long memberId, int page) {
        List<Long> ids = followRepository.findByFollowerMemberIdOrderByCreatedAtDescFollowIdDesc(memberId).stream()
                .map(Follow::getFollowingMemberId).toList();
        Set<Long> visible = memberReader.findVisibleIds(ids);
        List<Long> shown = ids.stream().filter(visible::contains).toList();
        PageRequest pageable = PageRequest.of(Math.max(page, 1) - 1, PostService.PAGE_SIZE);
        List<Long> pageIds = shown.stream().skip(pageable.getOffset()).limit(pageable.getPageSize()).toList();
        Map<Long, String> nicknames = memberReader.getNicknames(pageIds);
        return new PageImpl<>(pageIds.stream().map(id -> new MemberSummary(id, nicknames.get(id))).toList(), pageable, shown.size());
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

    /* 동료 리뷰 (#182·#183). U-19·U-20 은 portfolio 화면이라 이 값으로 버튼·받은 리뷰를 붙인다 (그 PR 은 portfolio 영역 리뷰) */

    /* 알림 도착지 (message API-NTF-02). 지운 리뷰도 작품 ID 를 돌려준다. 행이 없으면 빈 값 */
    public Optional<Long> findPortfolioIdOfReview(Long peerReviewId) {
        return peerReviewService.findPortfolioIdOfReview(peerReviewId);
    }

    /* U-20 받은 리뷰·도움됨·내 리뷰 수정 */
    public List<ReviewView> findReviewsOfPortfolio(Long portfolioId, Long viewerMemberId) {
        return peerReviewService.findReviewsOfPortfolio(portfolioId, viewerMemberId);
    }

    /* U-20 리뷰 요청·마감 버튼: 열린 요청이 있으면 그 ID */
    public Optional<Long> findOpenReviewRequestId(Long portfolioId) {
        return peerReviewService.findOpenReviewRequestId(portfolioId);
    }

    /* U-19 카드: 열린 리뷰 요청이 있는 작품 */
    public Set<Long> findPortfolioIdsWithOpenRequest(Collection<Long> portfolioIds) {
        return peerReviewService.findPortfolioIdsWithOpenRequest(portfolioIds);
    }
}

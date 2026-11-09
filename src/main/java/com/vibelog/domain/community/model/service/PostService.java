package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.CommentRepository;
import com.vibelog.domain.community.model.dao.PostCommentCount;
import com.vibelog.domain.community.model.dao.PostImageRepository;
import com.vibelog.domain.community.model.dao.FavoriteRepository;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.dto.CommentView;
import com.vibelog.domain.community.model.dto.PostDetail;
import com.vibelog.domain.community.model.dto.PostListItem;
import com.vibelog.domain.community.model.entity.Comment;
import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;
import com.vibelog.domain.community.model.entity.PostImage;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [PostService]
 * 역할 : 게시판 목록·게시글 상세 (API-CMT-01·02, FR-CMT-001·002).
 * 책임 : - 삭제된 글은 목록에서 빼고, 상세는 POST_NOT_FOUND 를 던진다.
 *        - 작성자 닉네임은 auth 의 MemberReader 로, 댓글 수는 한 번에 읽는다. 목록 20개에 글마다 쿼리가 나가지 않는다.
 *        - 글쓰기·수정·삭제는 #93, 댓글 등록·수정·삭제는 #94 에서 추가한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    public static final int PAGE_SIZE = 20;

    private final PostRepository postRepository;
    private final FavoriteRepository favoriteRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final MemberReader memberReader;

    /* page 는 화면 그대로 1부터 센다. q 는 제목에 들어 있는 글을 찾는다 */
    public Page<PostListItem> getPosts(PostCategory category, String q, int page) {
        String keyword = (q == null || q.isBlank()) ? null : q.strip();
        Set<Long> hidden = memberReader.findAllHiddenMemberIds();
        // 영구 정지 회원의 글은 목록에서 뺀다 (PL-RPT-006, #215). 없으면 IN () 이 되지 않게 0 을 넣는다 (회원 ID 는 1부터)
        Page<Post> posts = postRepository.search(category, keyword, hidden.isEmpty() ? Set.of(0L) : hidden,
                PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE));
        if (posts.isEmpty()) {
            return posts.map(post -> null);
        }
        List<Long> postIds = posts.map(Post::getPostId).toList();
        Map<Long, Long> commentCounts = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(PostCommentCount::postId, PostCommentCount::count));
        Map<Long, String> nicknames = memberReader.getNicknames(posts.map(Post::getMemberId).toSet());
        return posts.map(post -> PostListItem.from(post, nicknames.get(post.getMemberId()),
                commentCounts.getOrDefault(post.getPostId(), 0L)));
    }

    /* loginMemberId 는 비회원이면 null. 내 글이면 수정·삭제 버튼을, 회원이면 즐겨찾기 상태(API-CMT-02 P2)를 보여 준다 */
    public PostDetail getPost(Long postId, Long loginMemberId) {
        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
        if (!memberReader.findHiddenMemberIds(Set.of(post.getMemberId())).isEmpty()) {
            throw new BusinessException(CommunityErrorCode.HIDDEN_CONTENT);
        }
        List<String> imageUrls = postImageRepository.findByPostIdOrderBySortOrder(postId).stream()
                .map(PostImage::getImageUrl)
                .toList();
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAscCommentIdAsc(postId);

        Set<Long> memberIds = new HashSet<>();
        memberIds.add(post.getMemberId());
        comments.forEach(comment -> memberIds.add(comment.getMemberId()));
        Map<Long, String> nicknames = memberReader.getNicknames(memberIds);

        long commentCount = comments.stream().filter(comment -> !comment.isDeleted()).count();
        return PostDetail.of(post, nicknames.get(post.getMemberId()), imageUrls, commentCount,
                CommentView.tree(comments, nicknames, loginMemberId, memberReader.findHiddenMemberIds(memberIds)), Objects.equals(post.getMemberId(), loginMemberId),
                loginMemberId != null && favoriteRepository.existsByMemberIdAndPostId(loginMemberId, postId),
                memberReader.isVisible(post.getMemberId()));
    }
}

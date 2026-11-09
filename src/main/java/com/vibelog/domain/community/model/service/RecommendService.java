package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.PostRepository;
import com.vibelog.domain.community.model.dao.RecommendationRepository;
import com.vibelog.domain.community.model.dto.RankingItem;
import com.vibelog.domain.community.model.dto.RecommendResult;
import com.vibelog.domain.community.model.dto.WeeklyRecommendCount;
import com.vibelog.domain.community.model.entity.Post;
import com.vibelog.domain.community.model.entity.PostCategory;
import com.vibelog.domain.community.model.entity.Recommendation;
import com.vibelog.domain.message.model.policy.NotificationSender;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [RecommendService]
 * 역할 : 작품 공유 글 추천·취소와 이번 주 추천 랭킹 (API-CMT-11·12, FR-CMT-005·006).
 * 책임 : - 작품 공유 글만 (NOT_SHOWCASE, PL-CMT-001), 내 글은 안 됨 (SELF_RECOMMEND, PL-CMT-002). 추천은 이메일 인증 없이 된다.
 *        - 추가할 때만 글쓴이에게 RECOMMEND 알림 (계약 ⑬). 취소는 알리지 않는다.
 *        - 랭킹은 이번 주 월요일 0시부터 받은 추천 수로 센다 (PL-CMT-003). 동점·삭제 글 처리는 Repository 쿼리 (PL-CMT-004).
 *        - 영구 정지 회원의 글은 랭킹에서 뺀다 (PL-RPT-006).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendService {

    public static final int RANKING_SIZE = 10;

    private final RecommendationRepository recommendationRepository;
    private final PostRepository postRepository;
    private final MemberReader memberReader;
    private final NotificationSender notificationSender;

    @Transactional
    public RecommendResult toggle(Long memberId, Long postId) {
        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(CommunityErrorCode.POST_NOT_FOUND));
        if (post.getCategory() != PostCategory.SHOWCASE) {
            throw new BusinessException(CommunityErrorCode.NOT_SHOWCASE);
        }
        if (post.getMemberId().equals(memberId)) {
            throw new BusinessException(CommunityErrorCode.SELF_RECOMMEND);
        }
        boolean recommended = recommendationRepository.findByMemberIdAndPostId(memberId, postId)
                .map(recommendation -> {
                    recommendationRepository.delete(recommendation);
                    return false;
                })
                .orElseGet(() -> {
                    recommendationRepository.save(Recommendation.create(memberId, postId));
                    notificationSender.recommended(memberId, post.getMemberId(), postId, post.getTitle());
                    return true;
                });
        recommendationRepository.flush();
        return new RecommendResult(recommended, recommendationRepository.countByPostId(postId));
    }

    /* 상세 화면(U-22). 작품 공유 글이 아니면 추천을 보여 주지 않아 null. loginMemberId 는 비회원이면 null */
    public RecommendResult getStatus(Long postId, PostCategory category, Long loginMemberId) {
        if (category != PostCategory.SHOWCASE) {
            return null;
        }
        boolean recommended = loginMemberId != null && recommendationRepository.existsByMemberIdAndPostId(loginMemberId, postId);
        return new RecommendResult(recommended, recommendationRepository.countByPostId(postId));
    }

    public List<RankingItem> getWeeklyTop(int limit) {
        LocalDateTime monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        // 영구 정지 회원의 글은 순위에서 뺀다 (PL-RPT-006, #215). 빈 묶음이면 IN () 이 되지 않게 0 (회원 ID 는 1부터)
        Set<Long> hidden = memberReader.findAllHiddenMemberIds();
        List<WeeklyRecommendCount> counts = recommendationRepository.findWeeklyCounts(monday,
                hidden.isEmpty() ? Set.of(0L) : hidden, PageRequest.of(0, limit));
        List<Long> postIds = counts.stream().map(WeeklyRecommendCount::postId).toList();
        Map<Long, Post> posts = postRepository.findAllById(postIds).stream()
                .collect(Collectors.toMap(Post::getPostId, Function.identity()));
        Map<Long, String> nicknames = memberReader.getNicknames(posts.values().stream().map(Post::getMemberId).toList());

        List<RankingItem> items = new ArrayList<>();
        for (WeeklyRecommendCount count : counts) {
            Post post = posts.get(count.postId());
            items.add(new RankingItem(items.size() + 1, post.getPostId(), post.getTitle(),
                    nicknames.get(post.getMemberId()), count.count()));
        }
        return items;
    }
}

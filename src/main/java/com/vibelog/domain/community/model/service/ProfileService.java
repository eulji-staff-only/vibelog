package com.vibelog.domain.community.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.community.exception.CommunityErrorCode;
import com.vibelog.domain.community.model.dao.FollowRepository;
import com.vibelog.domain.community.model.dto.FollowResult;
import com.vibelog.domain.community.model.dto.ProfileView;
import com.vibelog.domain.community.model.entity.Follow;
import com.vibelog.domain.contest.model.service.ContestReader;
import com.vibelog.domain.message.model.service.MessageReader;
import com.vibelog.domain.portfolio.model.dto.PublicPortfolioCard;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/*
 * [ProfileService]
 * 역할 : 공개 프로필과 팔로우 (FR-CMT-008·009, API-CMT-14·15).
 * 책임 : - 볼 수 있는 회원(MemberReader.findVisibleIds)이 아니면 USER_NOT_FOUND. 나는 팔로우할 수 없다(SELF_FOLLOW).
 *        - 팔로우는 같은 요청으로 켜고 끈다. 팔로우 알림은 요구사항에 없어 보내지 않는다.
 *        - 게시글은 CommunityReader 와 같은 목록(지운 글 제외, 20개씩), 포트폴리오는 PortfolioReader 의 공개 포트폴리오,
 *          배지는 ContestReader 의 수상 기록.
 *        - 신고로 숨긴 게시글·포트폴리오는 다른 사람에게 빼고, 내 프로필이면 그대로 둔다 (FR-RPT-003, #256).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final FollowRepository followRepository;
    private final CommunityReader communityReader;
    private final MemberReader memberReader;
    private final PortfolioReader portfolioReader;
    private final ContestReader contestReader;
    private final MessageReader messageReader;

    public ProfileView getProfile(Long memberId, Long viewerMemberId, int page) {
        checkVisible(memberId);
        boolean me = memberId.equals(viewerMemberId);
        List<PublicPortfolioCard> portfolios = portfolioReader.findPublicOf(memberId);
        if (!me) {
            Set<Long> hidden = messageReader.findHiddenTargetIds("PORTFOLIO",
                    portfolios.stream().map(PublicPortfolioCard::portfolioId).toList());
            portfolios = portfolios.stream().filter(p -> !hidden.contains(p.portfolioId())).toList();
        }
        return new ProfileView(memberId, memberReader.getNickname(memberId), me,
                followRepository.existsByFollowerMemberIdAndFollowingMemberId(viewerMemberId, memberId),
                followRepository.countByFollowingMemberId(memberId), followRepository.countByFollowerMemberId(memberId),
                me ? communityReader.getPostsOf(memberId, page) : communityReader.getPublicPostsOf(memberId, page), portfolios,
                contestReader.findBadgesOf(memberId));
    }

    /* API-CMT-15. 바꾼 뒤 상태와 상대의 팔로워 수 */
    @Transactional
    public FollowResult toggleFollow(Long followerMemberId, Long followingMemberId) {
        if (followerMemberId.equals(followingMemberId)) {
            throw new BusinessException(CommunityErrorCode.SELF_FOLLOW);
        }
        checkVisible(followingMemberId);
        boolean following = followRepository.findByFollowerMemberIdAndFollowingMemberId(followerMemberId, followingMemberId)
                .map(follow -> {
                    followRepository.delete(follow);
                    return false;
                })
                .orElseGet(() -> {
                    followRepository.save(Follow.create(followerMemberId, followingMemberId));
                    return true;
                });
        return new FollowResult(following, followRepository.countByFollowingMemberId(followingMemberId));
    }

    private void checkVisible(Long memberId) {
        if (!memberReader.isVisible(memberId)) {
            throw new BusinessException(CommunityErrorCode.USER_NOT_FOUND);
        }
    }
}

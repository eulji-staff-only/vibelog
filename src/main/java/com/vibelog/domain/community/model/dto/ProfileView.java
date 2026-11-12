package com.vibelog.domain.community.model.dto;

import com.vibelog.domain.contest.model.dto.BadgeView;
import com.vibelog.domain.portfolio.model.dto.PublicPortfolioCard;
import org.springframework.data.domain.Page;

import java.util.List;

/*
 * 공개 프로필 (U-29, API-CMT-14). me 면 팔로우 버튼을 숨긴다. 배지는 콘테스트 수상 기록 (ContestReader, FR-RWD-002).
 * 쪽지 보내기(M-13)는 쪽지(#184)에서 더한다.
 */
public record ProfileView(Long memberId, String nickname, boolean me, boolean following, long followerCount,
                          long followingCount, Page<MemberPostItem> posts, List<PublicPortfolioCard> portfolios,
                          List<BadgeView> badges) {
}

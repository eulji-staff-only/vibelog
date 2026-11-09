package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.contest.model.dao.ContestAwardRepository;
import com.vibelog.domain.contest.model.dao.ContestRepository;
import com.vibelog.domain.contest.model.dto.BadgeView;
import com.vibelog.domain.contest.model.dto.ContestSummary;
import com.vibelog.domain.contest.model.entity.ContestAward;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/*
 * [ContestReader]
 * 역할 : 다른 영역에 주는 콘테스트 읽기 값 (contest 명세 "ContestReader", #176).
 * 책임 : 배지 목록(마이페이지·공개 프로필, FR-RWD-002), 이번 달 콘테스트(메인, API-USR-01). 읽기만 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestReader {

    private final ContestRepository contestRepository;
    private final ContestAwardRepository contestAwardRepository;

    /* 회원이 받은 배지, 최근 월부터 */
    public List<BadgeView> findBadgesOf(Long memberId) {
        return contestAwardRepository.findBadgeRowsOf(memberId).stream()
                .map(row -> {
                    String month = (String) row[1];
                    int rank = (Integer) row[2];
                    return new BadgeView((Long) row[0], ContestAward.badgeName(month, rank), month, rank);
                })
                .toList();
    }

    /* 이번 달(서버 시각 기준) 콘테스트. 없으면 빈 값 */
    public Optional<ContestSummary> findCurrent() {
        return contestRepository.findByContestMonth(YearMonth.now().toString())
                .map(c -> new ContestSummary(c.getContestId(), c.getTopic(), c.getEntryStartsAt(), c.getEntryEndsAt(),
                        c.getVoteStartsAt(), c.getVoteEndsAt()));
    }
}

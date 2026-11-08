package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.contest.exception.ContestErrorCode;
import com.vibelog.domain.contest.model.dao.ContestEntryRepository;
import com.vibelog.domain.contest.model.dao.ContestRepository;
import com.vibelog.domain.contest.model.dao.ContestVoteRepository;
import com.vibelog.domain.contest.model.dto.ContestCurrentView;
import com.vibelog.domain.contest.model.dto.ContestCurrentView.EntryCard;
import com.vibelog.domain.contest.model.dto.ContestCurrentView.MyEntry;
import com.vibelog.domain.contest.model.entity.Contest;
import com.vibelog.domain.contest.model.entity.ContestEntry;
import com.vibelog.domain.contest.model.entity.ContestVote;
import com.vibelog.domain.portfolio.model.dto.PortfolioBrief;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/*
 * [ContestService]
 * 역할 : 이달의 콘테스트 보기·출품·투표 (U-32, API-CTS-01~03, FR-CTS-001~003, #201).
 * 책임 : - 단계는 지금 시각과 운영자가 등록한 기간으로 정한다 (PL-CTS-001).
 *        - 출품: 콘테스트 → 출품 단계 → 포트폴리오(없음·삭제) → 내 것 → 공개 → 1인 1작품 순서로 검사 (PL-CTS-002).
 *        - 투표: 콘테스트 → 투표 단계 → 이메일 인증 → 출품작 → 내 작품 → 오늘 1표 → 같은 작품 1번 순서로 검사 (PL-CTS-003·004).
 *        - 동시에 두 번 눌러 DB 제약(uk_…)에 걸리면, 앞의 검사와 같은 에러로 답한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestService {

    private final ContestRepository contestRepository;
    private final ContestEntryRepository contestEntryRepository;
    private final ContestVoteRepository contestVoteRepository;
    private final PortfolioReader portfolioReader;
    private final MemberReader memberReader;

    /* 이번 달(서버 시각 기준) 콘테스트. 없으면 빈 값 → "이번 달 콘테스트를 준비하고 있습니다" */
    public Optional<ContestCurrentView> getCurrent(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        return contestRepository.findByContestMonth(YearMonth.from(now).toString()).map(contest -> toView(contest, memberId, now));
    }

    @Transactional
    public void enter(Long contestId, Long memberId, Long portfolioId) {
        Contest contest = findContest(contestId);
        if (!contest.isEntryOpen(LocalDateTime.now())) {
            throw new BusinessException(ContestErrorCode.ENTRY_CLOSED);
        }
        PortfolioBrief portfolio = portfolioReader.findBrief(portfolioId).filter(p -> !p.deleted())
                .orElseThrow(() -> new BusinessException(ContestErrorCode.PORTFOLIO_NOT_FOUND));
        if (!portfolio.memberId().equals(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        if (!portfolio.isPublic()) {
            throw new BusinessException(ContestErrorCode.PRIVATE_PORTFOLIO);
        }
        if (contestEntryRepository.findByContestIdAndMemberId(contestId, memberId).isPresent()) {
            throw new BusinessException(ContestErrorCode.ALREADY_ENTERED);
        }
        try {
            contestEntryRepository.saveAndFlush(ContestEntry.create(contestId, memberId, portfolioId));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ContestErrorCode.ALREADY_ENTERED); // 동시에 두 번: uk_contest_entry_contest_member
        }
    }

    @Transactional
    public void vote(Long contestId, Long memberId, Long contestEntryId) {
        Contest contest = findContest(contestId);
        LocalDateTime now = LocalDateTime.now();
        if (!contest.isVoteOpen(now)) {
            throw new BusinessException(ContestErrorCode.VOTE_CLOSED);
        }
        if (!memberReader.isEmailVerified(memberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
        ContestEntry entry = contestEntryRepository.findById(contestEntryId)
                .filter(e -> e.getContestId().equals(contestId) && !e.isCanceled())
                .orElseThrow(() -> new BusinessException(ContestErrorCode.ENTRY_NOT_FOUND));
        if (entry.getMemberId().equals(memberId)) {
            throw new BusinessException(ContestErrorCode.SELF_VOTE);
        }
        LocalDate today = now.toLocalDate();
        if (contestVoteRepository.existsByContestIdAndVoterMemberIdAndVoteDate(contestId, memberId, today)) {
            throw new BusinessException(ContestErrorCode.DAILY_VOTE_LIMIT);
        }
        if (contestVoteRepository.existsByContestEntryIdAndVoterMemberId(contestEntryId, memberId)) {
            throw new BusinessException(ContestErrorCode.ALREADY_VOTED);
        }
        try {
            contestVoteRepository.saveAndFlush(ContestVote.create(contestId, contestEntryId, memberId, today));
        } catch (DataIntegrityViolationException e) {
            // 위 검사를 통과한 뒤 걸리는 건 같은 날 동시에 들어온 두 번째 표다 (uk_contest_vote_daily)
            throw new BusinessException(ContestErrorCode.DAILY_VOTE_LIMIT);
        }
    }

    private Contest findContest(Long contestId) {
        return contestRepository.findById(contestId).orElseThrow(() -> new BusinessException(ContestErrorCode.CONTEST_NOT_FOUND));
    }

    private ContestCurrentView toView(Contest contest, Long memberId, LocalDateTime now) {
        Long contestId = contest.getContestId();
        boolean entryOpen = contest.isEntryOpen(now);
        boolean voteOpen = contest.isVoteOpen(now);

        MyEntry myEntry = null;
        List<PortfolioBrief> myPortfolios = List.of();
        if (entryOpen) {
            Optional<ContestEntry> mine = contestEntryRepository.findByContestIdAndMemberId(contestId, memberId);
            if (mine.isPresent()) {
                ContestEntry entry = mine.get();
                String title = portfolioReader.findBrief(entry.getPortfolioId()).map(PortfolioBrief::title).orElse("");
                myEntry = new MyEntry(entry.getPortfolioId(), title, entry.isCanceled());
            } else {
                myPortfolios = portfolioReader.findPublicBriefsOf(memberId);
            }
        }

        List<EntryCard> entries = List.of();
        boolean votedToday = false;
        boolean emailVerified = true;
        if (voteOpen) {
            entries = entryCards(contestId, memberId);
            votedToday = contestVoteRepository.existsByContestIdAndVoterMemberIdAndVoteDate(contestId, memberId, now.toLocalDate());
            emailVerified = memberReader.isEmailVerified(memberId);
        }

        return new ContestCurrentView(contestId, contest.getContestMonth(), contest.getTopic(), contest.getDescription(),
                contest.getEntryStartsAt(), contest.getEntryEndsAt(), contest.getVoteStartsAt(), contest.getVoteEndsAt(),
                entryOpen, voteOpen, contest.isVoteEnded(now), myEntry, myPortfolios, entries, votedToday, emailVerified);
    }

    /* 투표 보드: 취소되지 않은 출품작, 출품순. 비공개·삭제된 작품은 (출품 취소 전이라도) 다른 회원에게 보이지 않게 뺀다 */
    private List<EntryCard> entryCards(Long contestId, Long memberId) {
        List<ContestEntry> entries = contestEntryRepository.findByContestIdAndCanceledAtIsNullOrderByContestEntryIdAsc(contestId);
        Map<Long, PortfolioBrief> portfolios = portfolioReader.findBriefs(entries.stream().map(ContestEntry::getPortfolioId).toList());
        Map<Long, String> nicknames = memberReader.getNicknames(entries.stream().map(ContestEntry::getMemberId).toList());
        Set<Long> voted = new HashSet<>(contestVoteRepository.findEntryIdsVotedBy(contestId, memberId));
        return entries.stream()
                .filter(e -> {
                    PortfolioBrief p = portfolios.get(e.getPortfolioId());
                    return p != null && p.isPublic() && !p.deleted();
                })
                .map(e -> {
                    PortfolioBrief p = portfolios.get(e.getPortfolioId());
                    return new EntryCard(e.getContestEntryId(), p.portfolioId(), p.title(), p.summary(),
                            nicknames.getOrDefault(e.getMemberId(), ""), e.getMemberId().equals(memberId),
                            voted.contains(e.getContestEntryId()));
                })
                .toList();
    }
}

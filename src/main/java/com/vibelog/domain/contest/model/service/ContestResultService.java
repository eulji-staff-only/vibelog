package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.contest.exception.ContestErrorCode;
import com.vibelog.domain.contest.model.dao.ContestAwardRepository;
import com.vibelog.domain.contest.model.dao.ContestEntryRepository;
import com.vibelog.domain.contest.model.dao.ContestRepository;
import com.vibelog.domain.contest.model.dao.ContestVoteRepository;
import com.vibelog.domain.contest.model.dto.ContestResultView;
import com.vibelog.domain.contest.model.dto.ContestResultView.AwardItem;
import com.vibelog.domain.contest.model.dto.PastContestItem;
import com.vibelog.domain.contest.model.entity.Contest;
import com.vibelog.domain.contest.model.entity.ContestAward;
import com.vibelog.domain.contest.model.entity.ContestEntry;
import com.vibelog.domain.message.model.service.MessageReader;
import com.vibelog.domain.portfolio.model.dto.PortfolioBrief;
import com.vibelog.domain.portfolio.model.service.PortfolioReader;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [ContestResultService]
 * 역할 : 콘테스트 결과 보기 — U-32 결과 보드(지난 달), U-33 지난 콘테스트 목록·상세 (API-CTS-01·04·05, FR-CTS-005, #202).
 * 책임 : - 투표가 끝난 콘테스트만 보여 준다. 수상작은 집계 배치(ContestAwardService)가 남긴 contest_award 다.
 *        - 발표 뒤 비공개·삭제된 수상작은 순위·작성자는 두고 제목 대신 "비공개된 작품" (contest 명세 결정 14).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestResultService {

    static final int PAGE_SIZE = 10;
    static final String HIDDEN_TITLE = "비공개된 작품";

    private final ContestRepository contestRepository;
    private final ContestEntryRepository contestEntryRepository;
    private final ContestVoteRepository contestVoteRepository;
    private final ContestAwardRepository contestAwardRepository;
    private final PortfolioReader portfolioReader;
    private final MemberReader memberReader;
    private final MessageReader messageReader;
    private final ContestAwardService contestAwardService;

    /* U-32 결과 보드: 지난 달 콘테스트 (이번 달 결과는 다음 달 1일에 나온다, 결정 12). 없거나 투표 중이면 빈 값 */
    public Optional<ContestResultView> findLastMonthResult() {
        LocalDateTime now = LocalDateTime.now();
        return contestRepository.findByContestMonth(YearMonth.from(now).minusMonths(1).toString())
                .filter(contest -> contest.isVoteEnded(now))
                .map(this::toResult);
    }

    /* API-CTS-05. 없거나 투표가 끝나기 전이면 CONTEST_NOT_FOUND */
    public ContestResultView getResult(Long contestId) {
        return contestRepository.findById(contestId)
                .filter(contest -> contest.isVoteEnded(LocalDateTime.now()))
                .map(this::toResult)
                .orElseThrow(() -> new BusinessException(ContestErrorCode.CONTEST_NOT_FOUND));
    }

    /* API-CTS-04. 투표가 끝난 콘테스트, 최근 월부터 10개씩 (page 는 1부터) */
    public Page<PastContestItem> getPast(int page) {
        Page<Contest> contests = contestRepository.findByVoteEndsAtLessThanEqualOrderByContestMonthDesc(LocalDateTime.now(),
                PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE));
        List<ContestAward> firsts = contests.isEmpty() ? List.of()
                : contestAwardRepository.findByContestIdInAndAwardRankOrderByContestEntryIdAsc(
                        contests.stream().map(Contest::getContestId).toList(), 1);
        Map<Long, ContestEntry> entries = entriesOf(firsts);
        List<Long> portfolioIds = entries.values().stream().map(ContestEntry::getPortfolioId).toList();
        Map<Long, PortfolioBrief> portfolios = portfolioReader.findBriefs(portfolioIds);
        Set<Long> hidden = messageReader.findHiddenTargetIds("PORTFOLIO", portfolioIds);
        Map<Long, String> firstPlace = firsts.stream().collect(Collectors.groupingBy(ContestAward::getContestId,
                Collectors.mapping(a -> {
                    Long portfolioId = entries.get(a.getContestEntryId()).getPortfolioId();
                    return hidden.contains(portfolioId) ? MessageReader.HIDDEN_TEXT : titleOrHidden(portfolios.get(portfolioId));
                }, Collectors.joining(", "))));
        LocalDateTime now = LocalDateTime.now();
        return contests.map(c -> {
            String first = firstPlace.get(c.getContestId());
            return new PastContestItem(c.getContestId(), c.getContestMonth(), c.getTopic(), first,
                    first == null && contestAwardService.isHeld(c, now));
        });
    }

    private ContestResultView toResult(Contest contest) {
        List<ContestAward> awards = contestAwardRepository.findByContestIdOrderByAwardRankAscContestEntryIdAsc(contest.getContestId());
        Map<Long, ContestEntry> entries = entriesOf(awards);
        List<Long> portfolioIds = entries.values().stream().map(ContestEntry::getPortfolioId).toList();
        Map<Long, PortfolioBrief> portfolios = portfolioReader.findBriefs(portfolioIds);
        // 신고로 숨긴 수상작은 순위·작성자는 두고 제목 대신 숨김 문구, 링크 없음 (FR-RPT-003, message 명세)
        Set<Long> hidden = messageReader.findHiddenTargetIds("PORTFOLIO", portfolioIds);
        Map<Long, String> nicknames = memberReader.getNicknames(entries.values().stream().map(ContestEntry::getMemberId).toList());
        Map<Long, Long> votes = awards.isEmpty() ? Map.of() : contestVoteRepository.countByEntry(contest.getContestId()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        List<AwardItem> items = awards.stream().map(a -> {
            ContestEntry entry = entries.get(a.getContestEntryId());
            PortfolioBrief portfolio = portfolios.get(entry.getPortfolioId());
            boolean reportHidden = hidden.contains(entry.getPortfolioId());
            return new AwardItem(a.getAwardRank(), entry.getPortfolioId(), reportHidden ? null : visibleTitle(portfolio),
                    nicknames.getOrDefault(entry.getMemberId(), ""), votes.getOrDefault(a.getContestEntryId(), 0L), reportHidden);
        }).toList();
        return new ContestResultView(contest.getContestId(), contest.getContestMonth(), contest.getTopic(), contest.getDescription(), items,
                items.isEmpty() && contestAwardService.isHeld(contest, LocalDateTime.now()));
    }

    private Map<Long, ContestEntry> entriesOf(Collection<ContestAward> awards) {
        return contestEntryRepository.findAllById(awards.stream().map(ContestAward::getContestEntryId).toList()).stream()
                .collect(Collectors.toMap(ContestEntry::getContestEntryId, Function.identity()));
    }

    /* 공개이고 삭제되지 않은 작품만 제목을 보여 준다. 아니면 null (화면은 "비공개된 작품", 링크 없음) */
    private static String visibleTitle(PortfolioBrief portfolio) {
        return portfolio != null && portfolio.isPublic() && !portfolio.deleted() ? portfolio.title() : null;
    }

    private static String titleOrHidden(PortfolioBrief portfolio) {
        return Objects.requireNonNullElse(visibleTitle(portfolio), HIDDEN_TITLE);
    }
}

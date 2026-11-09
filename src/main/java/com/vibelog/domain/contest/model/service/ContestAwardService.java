package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.contest.model.dao.ContestAwardRepository;
import com.vibelog.domain.contest.model.dao.ContestEntryRepository;
import com.vibelog.domain.contest.model.dao.ContestRepository;
import com.vibelog.domain.contest.model.dao.ContestVoteRepository;
import com.vibelog.domain.contest.model.entity.Contest;
import com.vibelog.domain.contest.model.entity.ContestAward;
import com.vibelog.domain.contest.model.entity.ContestEntry;
import com.vibelog.domain.message.model.policy.NotificationSender;
import com.vibelog.domain.message.model.service.MessageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * [ContestAwardService]
 * 역할 : 콘테스트 결과 발표와 배지 지급 (FR-CTS-004, FR-RWD-001, PL-CTS-005·006, #202).
 * 책임 : - 10분마다 투표가 끝났고 수상 기록이 없는 콘테스트를 집계한다 (contest 명세 결정 7). 배치가 밀려도 다음에 집계한다.
 *          투표가 끝나면 표·출품 취소를 더 받지 않고 회원 상태도 투표가 끝난 시각으로 본다 (결정 13). 신고 숨김만 아래처럼 예외다.
 *        - 대상: 출품 취소되지 않았고, 투표가 끝난 시각에 정지 중이 아니었고 탈퇴하지 않은 회원의 출품작 중 1표 이상 받은 것 (결정 6·8).
 *        - 순위: 표 수 많은 순, 같은 표 수는 같은 순위(1·1·2), 순위마다 먼저 출품한 2명까지, 3등까지 (결정 5).
 *        - 수상 1건 = contest_award 1행 = 배지. 수상자에게 BADGE 알림 (계약 ⑬).
 *        - 신고 숨김 (PL-CTS-006, 결정 16, #295): 운영자가 숨김 확정한 출품작은 빼고 순위를 매긴다. 기한(투표 끝 + 3일) 전에
 *          1·2·3등 중 자동 숨김이 있으면 수상 기록을 남기지 않고(보류) 다음 배치에서 다시 본다. 숨김 확정은
 *          min(지금, 기한)까지 처리된 것만 보므로 기한 뒤 확정은 시상에 반영하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ContestAwardService {

    static final int MAX_RANK = 3;
    static final int MAX_PER_RANK = 2;
    /* 투표가 끝난 때부터 시상을 미룰 수 있는 기한 (PL-CTS-006) */
    static final int HOLD_DAYS = 3;
    private static final String PORTFOLIO = "PORTFOLIO";

    private final ContestRepository contestRepository;
    private final ContestEntryRepository contestEntryRepository;
    private final ContestVoteRepository contestVoteRepository;
    private final ContestAwardRepository contestAwardRepository;
    private final MemberReader memberReader;
    private final NotificationSender notificationSender;
    private final MessageReader messageReader;

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void announce() {
        LocalDateTime now = LocalDateTime.now();
        contestRepository.findToAnnounce(now).forEach(contest -> decide(contest, now).ifPresent(ranks -> award(contest, ranks)));
    }

    /* 투표가 끝났고 수상 기록이 없는 콘테스트가 시상 보류 중인가. 결과 화면 안내에 쓴다 (contest 명세 결정 17) */
    public boolean isHeld(Contest contest, LocalDateTime now) {
        return now.isBefore(deadlineOf(contest)) && decide(contest, now).isEmpty();
    }

    private void award(Contest contest, Map<ContestEntry, Integer> ranks) {
        ranks.forEach((entry, rank) -> {
            ContestAward award = contestAwardRepository.save(ContestAward.create(contest.getContestId(), entry.getContestEntryId(), rank));
            notificationSender.badgeAwarded(entry.getMemberId(), award.getContestAwardId(),
                    ContestAward.badgeName(contest.getContestMonth(), rank));
        });
    }

    /* 시상할 출품작 → 순위 (출품순). 시상 보류면 빈 값 */
    private Optional<Map<ContestEntry, Integer>> decide(Contest contest, LocalDateTime now) {
        Map<Long, Long> votes = contestVoteRepository.countByEntry(contest.getContestId()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        // 출품순. 표를 받은 것만 보면 되므로 회원·신고 검사도 그 출품작만 한다
        List<ContestEntry> voted = contestEntryRepository.findByContestIdAndCanceledAtIsNullOrderByContestEntryIdAsc(contest.getContestId())
                .stream()
                .filter(e -> votes.containsKey(e.getContestEntryId()))
                .toList();
        List<Long> memberIds = voted.stream().map(ContestEntry::getMemberId).toList();
        Set<Long> excluded = new HashSet<>(memberReader.findSuspendedMemberIds(memberIds, contest.getVoteEndsAt()));
        excluded.addAll(memberReader.findWithdrawnMemberIds(memberIds, contest.getVoteEndsAt()));
        LocalDateTime deadline = deadlineOf(contest);
        boolean beforeDeadline = now.isBefore(deadline);
        Set<Long> confirmedHidden = messageReader.findConfirmedHiddenTargetIds(PORTFOLIO,
                voted.stream().map(ContestEntry::getPortfolioId).toList(), beforeDeadline ? now : deadline);
        Map<Long, ContestEntry> candidates = voted.stream()
                .filter(e -> !excluded.contains(e.getMemberId()) && !confirmedHidden.contains(e.getPortfolioId()))
                .collect(Collectors.toMap(ContestEntry::getContestEntryId, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        Map<ContestEntry, Integer> ranks = new LinkedHashMap<>();
        rank(List.copyOf(candidates.keySet()), votes).forEach((entryId, rank) -> ranks.put(candidates.get(entryId), rank));
        if (beforeDeadline && !messageReader.findAutoHiddenTargetIds(PORTFOLIO,
                ranks.keySet().stream().map(ContestEntry::getPortfolioId).toList()).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ranks);
    }

    private static LocalDateTime deadlineOf(Contest contest) {
        return contest.getVoteEndsAt().plusDays(HOLD_DAYS);
    }

    /*
     * 출품순 출품작 ID → (출품작 ID → 순위). 0표는 빼고, 표 수 많은 순으로 같은 표 수는 같은 순위,
     * 다음 순위는 바로 다음 표 수. 순위마다 먼저 출품한 MAX_PER_RANK 명, MAX_RANK 등까지.
     */
    static Map<Long, Integer> rank(List<Long> entryIdsInEntryOrder, Map<Long, Long> votes) {
        List<Long> sorted = entryIdsInEntryOrder.stream()
                .filter(id -> votes.getOrDefault(id, 0L) > 0)
                .sorted(Comparator.comparing((Long id) -> votes.get(id)).reversed()) // 안정 정렬이라 같은 표 수는 출품순 그대로
                .toList();
        Map<Long, Integer> ranks = new LinkedHashMap<>();
        int rank = 0;
        long previousVotes = -1;
        int inRank = 0;
        for (Long id : sorted) {
            long count = votes.get(id);
            if (count != previousVotes) {
                rank++;
                previousVotes = count;
                inRank = 0;
            }
            if (rank > MAX_RANK) {
                break;
            }
            if (inRank++ < MAX_PER_RANK) {
                ranks.put(id, rank);
            }
        }
        return ranks;
    }
}

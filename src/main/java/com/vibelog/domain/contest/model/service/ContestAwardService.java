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
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [ContestAwardService]
 * 역할 : 콘테스트 결과 발표와 배지 지급 (FR-CTS-004, FR-RWD-001, PL-CTS-005·006, #202).
 * 책임 : - 10분마다 투표가 끝난 지 하루가 안 됐고 수상 기록이 없는 콘테스트를 집계한다 (contest 명세 결정 7).
 *          투표가 끝나면 표·출품 취소를 더 받지 않으므로 언제 돌아도 결과가 같다 (결정 13).
 *        - 대상: 출품 취소되지 않았고, 지금 정지 중이거나 탈퇴하지 않은 회원의 출품작 중 1표 이상 받은 것 (결정 6·8).
 *        - 순위: 표 수 많은 순, 같은 표 수는 같은 순위(1·1·2), 순위마다 먼저 출품한 2명까지, 3등까지 (결정 5).
 *        - 수상 1건 = contest_award 1행 = 배지. 수상자에게 BADGE 알림 (계약 ⑬).
 */
@Service
@RequiredArgsConstructor
public class ContestAwardService {

    static final int MAX_RANK = 3;
    static final int MAX_PER_RANK = 2;

    private final ContestRepository contestRepository;
    private final ContestEntryRepository contestEntryRepository;
    private final ContestVoteRepository contestVoteRepository;
    private final ContestAwardRepository contestAwardRepository;
    private final MemberReader memberReader;
    private final NotificationSender notificationSender;

    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void announce() {
        LocalDateTime now = LocalDateTime.now();
        contestRepository.findToAnnounce(now, now.minusDays(1)).forEach(this::award);
    }

    private void award(Contest contest) {
        Long contestId = contest.getContestId();
        Map<Long, Long> votes = contestVoteRepository.countByEntry(contestId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        // 출품순. 표를 받은 것만 보면 되므로 회원 검사도 그것만 한다
        List<ContestEntry> voted = contestEntryRepository.findByContestIdAndCanceledAtIsNullOrderByContestEntryIdAsc(contestId).stream()
                .filter(e -> votes.containsKey(e.getContestEntryId()))
                .toList();
        Set<Long> suspended = memberReader.findSuspendedMemberIds(voted.stream().map(ContestEntry::getMemberId).toList());
        Map<Long, Long> memberOfEntry = voted.stream()
                .filter(e -> !suspended.contains(e.getMemberId()) && memberReader.isActive(e.getMemberId()))
                .collect(Collectors.toMap(ContestEntry::getContestEntryId, ContestEntry::getMemberId, (a, b) -> a, LinkedHashMap::new));

        rank(List.copyOf(memberOfEntry.keySet()), votes).forEach((entryId, rank) -> {
            ContestAward award = contestAwardRepository.save(ContestAward.create(contestId, entryId, rank));
            notificationSender.badgeAwarded(memberOfEntry.get(entryId), award.getContestAwardId(),
                    ContestAward.badgeName(contest.getContestMonth(), rank));
        });
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

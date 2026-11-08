package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.contest.exception.ContestErrorCode;
import com.vibelog.domain.contest.model.dao.ContestAwardRepository;
import com.vibelog.domain.contest.model.dao.ContestEntryRepository;
import com.vibelog.domain.contest.model.dao.ContestRepository;
import com.vibelog.domain.contest.model.dto.ContestAdminItem;
import com.vibelog.domain.contest.model.dto.ContestForm;
import com.vibelog.domain.contest.model.entity.Contest;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * [ContestAdminService]
 * 역할 : 백오피스 콘테스트 관리 (A-11, API-CTS-06~08, FR-CTS-006, #200).
 * 책임 : - 한 달에 하나 (CONTEST_MONTH_EXISTS, uk_contest_contest_month). 수정할 때는 자기 자신을 뺀다.
 *        - 출품이 시작된 뒤에는 월·기간을 바꿀 수 없다 (CONTEST_STARTED, 명세 결정 11). 주제·설명은 바꿀 수 있다.
 *        - 삭제는 요구사항·화면에 없어 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestAdminService {

    static final int PAGE_SIZE = 10;

    private final ContestRepository contestRepository;
    private final ContestEntryRepository contestEntryRepository;
    private final ContestAwardRepository contestAwardRepository;

    public Page<ContestAdminItem> getPage(int page) {
        Page<Contest> contests = contestRepository.findPageByOrderByContestMonthDesc(PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE));
        List<Long> ids = contests.map(Contest::getContestId).toList();
        Map<Long, Long> entryCounts = ids.isEmpty() ? Map.of() : contestEntryRepository.countLiveByContestIds(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Set<Long> announced = ids.isEmpty() ? Set.of() : new HashSet<>(contestAwardRepository.findAnnouncedIn(ids));
        return contests.map(c -> new ContestAdminItem(c.getContestId(), c.getContestMonth(), c.getTopic(), c.getEntryStartsAt(),
                c.getEntryEndsAt(), c.getVoteStartsAt(), c.getVoteEndsAt(), entryCounts.getOrDefault(c.getContestId(), 0L),
                announced.contains(c.getContestId())));
    }

    /* 새 폼은 다음 달 기본값 (API-CTS-06) */
    public ContestForm newForm() {
        return ContestForm.defaultsFor(YearMonth.now().plusMonths(1));
    }

    public ContestForm getForm(Long contestId) {
        return ContestForm.from(find(contestId));
    }

    @Transactional
    public Long create(ContestForm form) {
        if (contestRepository.existsByContestMonth(form.getContestMonth())) {
            throw new BusinessException(ContestErrorCode.CONTEST_MONTH_EXISTS);
        }
        return contestRepository.save(Contest.create(form.getContestMonth(), form.getTopic().strip(), form.getDescription().strip(),
                form.getEntryStartsAt(), form.getEntryEndsAt(), form.getVoteStartsAt(), form.getVoteEndsAt())).getContestId();
    }

    @Transactional
    public void update(Long contestId, ContestForm form) {
        Contest contest = find(contestId);
        boolean rescheduled = !contest.getContestMonth().equals(form.getContestMonth())
                || !Objects.equals(contest.getEntryStartsAt(), form.getEntryStartsAt())
                || !Objects.equals(contest.getEntryEndsAt(), form.getEntryEndsAt())
                || !Objects.equals(contest.getVoteStartsAt(), form.getVoteStartsAt())
                || !Objects.equals(contest.getVoteEndsAt(), form.getVoteEndsAt());
        if (rescheduled) {
            if (contest.isStarted(LocalDateTime.now())) {
                throw new BusinessException(ContestErrorCode.CONTEST_STARTED);
            }
            if (!contest.getContestMonth().equals(form.getContestMonth()) && contestRepository.existsByContestMonth(form.getContestMonth())) {
                throw new BusinessException(ContestErrorCode.CONTEST_MONTH_EXISTS);
            }
            contest.reschedule(form.getContestMonth(), form.getEntryStartsAt(), form.getEntryEndsAt(), form.getVoteStartsAt(),
                    form.getVoteEndsAt());
        }
        contest.changeInfo(form.getTopic().strip(), form.getDescription().strip());
    }

    private Contest find(Long contestId) {
        return contestRepository.findById(contestId).orElseThrow(() -> new BusinessException(ContestErrorCode.CONTEST_NOT_FOUND));
    }
}

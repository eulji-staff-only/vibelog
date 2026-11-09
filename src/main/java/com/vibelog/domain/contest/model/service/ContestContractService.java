package com.vibelog.domain.contest.model.service;

import com.vibelog.domain.contest.model.dao.ContestEntryRepository;
import com.vibelog.domain.contest.model.entity.ContestEntry;
import com.vibelog.domain.contest.model.policy.ContestEntryCanceler;
import com.vibelog.domain.contest.model.policy.ContestEntryChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/*
 * [ContestContractService]
 * 역할 : contest 가 portfolio 에 주는 계약 ⑮ ContestEntryChecker, ⑯ ContestEntryCanceler 구현 (#176).
 * 책임 : 둘 다 "출품 중"(취소 안 됨 + 투표가 끝나기 전)인 출품만 본다. 발표 뒤에는 수정·삭제가 자유롭다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestContractService implements ContestEntryChecker, ContestEntryCanceler {

    private final ContestEntryRepository contestEntryRepository;

    @Override
    public boolean isEntered(Long portfolioId) {
        return !contestEntryRepository.findOngoingByPortfolioId(portfolioId, LocalDateTime.now()).isEmpty();
    }

    @Override
    @Transactional
    public void cancelEntryOf(Long portfolioId) {
        contestEntryRepository.findOngoingByPortfolioId(portfolioId, LocalDateTime.now()).forEach(ContestEntry::cancel);
    }
}

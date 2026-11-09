package com.vibelog.domain.contest.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [ContestAward]
 * 역할 : 수상 1건 = 배지 (contest_award, FR-CTS-004, FR-RWD-001). 순위는 1~3 (ck_contest_award_rank).
 * 책임 : 배지 이름은 contest 명세대로 "{yyyy}년 {M}월 {순위}등" (badgeName).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContestAward extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contestAwardId;

    private Long contestId;

    private Long contestEntryId;

    private int awardRank;

    private ContestAward(Long contestId, Long contestEntryId, int awardRank) {
        this.contestId = contestId;
        this.contestEntryId = contestEntryId;
        this.awardRank = awardRank;
    }

    public static ContestAward create(Long contestId, Long contestEntryId, int awardRank) {
        return new ContestAward(contestId, contestEntryId, awardRank);
    }

    /* "2026-11", 1 → "2026년 11월 1등" */
    public static String badgeName(String contestMonth, int awardRank) {
        return contestMonth.substring(0, 4) + "년 " + Integer.parseInt(contestMonth.substring(5, 7)) + "월 " + awardRank + "등";
    }
}

package com.vibelog.domain.contest.model.entity;

import com.vibelog.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * [ContestEntry]
 * 역할 : 출품 1건 (contest_entry, FR-CTS-002). 1인 1작품은 uk_contest_entry_contest_member 가 막는다 (PL-CTS-002).
 * 책임 : 출품작을 비공개·삭제하면 취소(canceled_at)되고 받은 표는 무효다 (PL-CTS-006, 계약 ⑯).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContestEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contestEntryId;

    private Long contestId;

    private Long memberId;

    private Long portfolioId;

    private LocalDateTime canceledAt;

    private ContestEntry(Long contestId, Long memberId, Long portfolioId) {
        this.contestId = contestId;
        this.memberId = memberId;
        this.portfolioId = portfolioId;
    }

    public static ContestEntry create(Long contestId, Long memberId, Long portfolioId) {
        return new ContestEntry(contestId, memberId, portfolioId);
    }

    public boolean isCanceled() {
        return canceledAt != null;
    }

    public void cancel() {
        if (canceledAt == null) {
            canceledAt = LocalDateTime.now();
        }
    }
}

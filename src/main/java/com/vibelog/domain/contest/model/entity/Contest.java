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
 * [Contest]
 * 역할 : 월간 콘테스트 (contest, FR-CTS-001·006). 운영자가 월·주제·출품·투표 기간을 등록한다 (PL-CTS-001).
 * 책임 : 단계(출품·투표·투표 끝)는 지금 시각과 기간으로 판단한다. 기간 순서는 ck_contest_period 가 지킨다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Contest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long contestId;

    private String contestMonth;    // yyyy-MM

    private String topic;

    private String description;

    private LocalDateTime entryStartsAt;

    private LocalDateTime entryEndsAt;

    private LocalDateTime voteStartsAt;

    private LocalDateTime voteEndsAt;

    private Contest(String contestMonth, String topic, String description, LocalDateTime entryStartsAt,
                    LocalDateTime entryEndsAt, LocalDateTime voteStartsAt, LocalDateTime voteEndsAt) {
        this.contestMonth = contestMonth;
        this.topic = topic;
        this.description = description;
        this.entryStartsAt = entryStartsAt;
        this.entryEndsAt = entryEndsAt;
        this.voteStartsAt = voteStartsAt;
        this.voteEndsAt = voteEndsAt;
    }

    public static Contest create(String contestMonth, String topic, String description, LocalDateTime entryStartsAt,
                                 LocalDateTime entryEndsAt, LocalDateTime voteStartsAt, LocalDateTime voteEndsAt) {
        return new Contest(contestMonth, topic, description, entryStartsAt, entryEndsAt, voteStartsAt, voteEndsAt);
    }

    /* API-CTS-08: 주제·설명은 언제든 바꾼다 */
    public void changeInfo(String topic, String description) {
        this.topic = topic;
        this.description = description;
    }

    /* API-CTS-08: 월·기간은 출품이 시작되기 전에만 바꾼다 (Service 가 CONTEST_STARTED 로 막는다) */
    public void reschedule(String contestMonth, LocalDateTime entryStartsAt, LocalDateTime entryEndsAt,
                           LocalDateTime voteStartsAt, LocalDateTime voteEndsAt) {
        this.contestMonth = contestMonth;
        this.entryStartsAt = entryStartsAt;
        this.entryEndsAt = entryEndsAt;
        this.voteStartsAt = voteStartsAt;
        this.voteEndsAt = voteEndsAt;
    }

    public boolean isStarted(LocalDateTime now) {
        return !now.isBefore(entryStartsAt);
    }

    public boolean isEntryOpen(LocalDateTime now) {
        return !now.isBefore(entryStartsAt) && now.isBefore(entryEndsAt);
    }

    public boolean isVoteOpen(LocalDateTime now) {
        return !now.isBefore(voteStartsAt) && now.isBefore(voteEndsAt);
    }

    public boolean isVoteEnded(LocalDateTime now) {
        return !now.isBefore(voteEndsAt);
    }
}

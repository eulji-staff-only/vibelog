package com.vibelog.domain.contest.model.dto;

import com.vibelog.domain.contest.model.entity.Contest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.YearMonth;

/*
 * 콘테스트 등록·수정 폼 (API-CTS-07·08 form contestMonth, topic, description, entry·vote 기간).
 * 기간 순서는 출품 시작 < 출품 끝 ≤ 투표 시작 < 투표 끝 (ck_contest_period).
 * 새 폼은 다음 달과 PL-CTS-001 기본 기간(1일 0시 ~ 21일 0시 출품, 21일 0시 ~ 다음 달 1일 0시 투표)으로 채운다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ContestForm {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm";
    static final int VOTE_START_DAY = 21;   // 화면 contest-admin.js 도 같은 기본값을 쓴다 (#401)
    static final String MONTH_REGEX = "\\d{4}-(0[1-9]|1[0-2])";

    @NotBlank(message = "월을 골라 주세요")
    @Pattern(regexp = MONTH_REGEX, message = "월은 2026-12 처럼 입력해 주세요")
    private String contestMonth;

    @NotBlank(message = "주제를 입력해 주세요")
    @Size(max = 200, message = "주제는 200자까지 쓸 수 있습니다")
    private String topic;

    @NotBlank(message = "설명을 입력해 주세요")
    private String description;

    @NotNull(message = "출품 시작을 입력해 주세요")
    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime entryStartsAt;

    @NotNull(message = "출품 끝을 입력해 주세요")
    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime entryEndsAt;

    @NotNull(message = "투표 시작을 입력해 주세요")
    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime voteStartsAt;

    @NotNull(message = "투표 끝을 입력해 주세요")
    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime voteEndsAt;

    @AssertTrue(message = "기간은 출품 시작 < 출품 끝 ≤ 투표 시작 < 투표 끝 순서여야 합니다")
    public boolean isPeriodValid() {
        if (entryStartsAt == null || entryEndsAt == null || voteStartsAt == null || voteEndsAt == null) {
            return true; // 빈 칸은 @NotNull 이 알린다
        }
        return entryStartsAt.isBefore(entryEndsAt) && !voteStartsAt.isBefore(entryEndsAt) && voteStartsAt.isBefore(voteEndsAt);
    }

    /* 이달의 콘테스트는 contest_month 로 찾으므로 모든 기간이 그 달 1일 0시 ~ 다음 달 1일 0시 안이어야 한다 (contest 명세 결정 15) */
    @AssertTrue(message = "모든 기간은 그 달 1일 0시부터 다음 달 1일 0시 안이어야 합니다")
    public boolean isWithinMonth() {
        if (contestMonth == null || !contestMonth.matches(MONTH_REGEX) || entryStartsAt == null || voteEndsAt == null) {
            return true; // 빈 칸·형식은 다른 검사가 알린다
        }
        YearMonth month = YearMonth.parse(contestMonth);
        return !entryStartsAt.isBefore(month.atDay(1).atStartOfDay()) && !voteEndsAt.isAfter(month.plusMonths(1).atDay(1).atStartOfDay());
    }

    /* 새 폼: 그 달과 PL-CTS-001 기본 기간 */
    public static ContestForm defaultsFor(YearMonth month) {
        ContestForm form = new ContestForm();
        form.contestMonth = month.toString();
        form.entryStartsAt = month.atDay(1).atStartOfDay();
        form.entryEndsAt = month.atDay(VOTE_START_DAY).atStartOfDay();
        form.voteStartsAt = form.entryEndsAt;
        form.voteEndsAt = month.plusMonths(1).atDay(1).atStartOfDay();
        return form;
    }

    public static ContestForm from(Contest contest) {
        ContestForm form = new ContestForm();
        form.contestMonth = contest.getContestMonth();
        form.topic = contest.getTopic();
        form.description = contest.getDescription();
        form.entryStartsAt = contest.getEntryStartsAt();
        form.entryEndsAt = contest.getEntryEndsAt();
        form.voteStartsAt = contest.getVoteStartsAt();
        form.voteEndsAt = contest.getVoteEndsAt();
        return form;
    }
}

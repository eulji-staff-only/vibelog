package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.MemberSuspension;
import com.vibelog.domain.auth.model.entity.SuspensionLevel;

import java.time.LocalDateTime;
import java.util.List;

/*
 * 운영자 회원 상세(A-08)의 이용 제재 칸 (API-ADM-38 P2).
 * current 는 지금 정지 중인 제재(없으면 null), suggestedLevel 은 모달의 기본 선택(지난 제재의 다음 단계, 처음이면 경고).
 */
public record AdminSanction(Record current, boolean suspendable, SuspensionLevel suggestedLevel, List<Record> history) {

    public record Record(String levelLabel, String reason, LocalDateTime startsAt, LocalDateTime endsAt,
                         LocalDateTime releasedAt) {

        static Record from(MemberSuspension suspension) {
            return new Record(suspension.getLevel().getLabel(), suspension.getReason(), suspension.getStartsAt(),
                    suspension.getEndsAt(), suspension.getReleasedAt());
        }
    }

    public static AdminSanction of(MemberSuspension current, boolean suspendable, List<MemberSuspension> history) {
        SuspensionLevel suggested = history.isEmpty() ? SuspensionLevel.WARNING : history.get(0).getLevel().next();
        return new AdminSanction(current == null ? null : Record.from(current), suspendable, suggested,
                history.stream().map(Record::from).toList());
    }
}

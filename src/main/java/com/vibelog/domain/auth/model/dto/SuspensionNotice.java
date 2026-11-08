package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.MemberSuspension;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * 이용 정지 안내 (M-07). 로그인 화면에 flash 로 넘기므로 세션에 담길 수 있게 Serializable.
 * endsAt 이 null 이면 영구 정지다.
 */
public record SuspensionNotice(String levelLabel, String reason, LocalDateTime endsAt) implements Serializable {

    public static SuspensionNotice from(MemberSuspension suspension) {
        return new SuspensionNotice(suspension.getLevel().getLabel(), suspension.getReason(), suspension.getEndsAt());
    }
}

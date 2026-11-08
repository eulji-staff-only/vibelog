package com.vibelog.domain.auth.model.dto;

import com.vibelog.domain.auth.model.entity.Member;

import java.time.LocalDateTime;

/* 운영자 회원 목록 한 줄 (API-ADM-37) */
public record AdminMemberItem(Long memberId, String nickname, String email, String roleLabel, boolean emailVerified,
                              LocalDateTime joinedAt) {

    public static AdminMemberItem from(Member member) {
        return new AdminMemberItem(member.getMemberId(), member.getNickname(), member.getEmail(),
                AdminMemberDetail.roleLabel(member), member.getEmailVerifiedAt() != null, member.getCreatedAt());
    }
}

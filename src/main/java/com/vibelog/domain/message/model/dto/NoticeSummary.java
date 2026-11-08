package com.vibelog.domain.message.model.dto;

import com.vibelog.domain.message.model.entity.Notice;

import java.time.LocalDateTime;

/* 공지 한 줄 (메인 화면 최근 공지, API-USR-01) */
public record NoticeSummary(Long noticeId, String title, LocalDateTime createdAt) {

    public static NoticeSummary from(Notice notice) {
        return new NoticeSummary(notice.getNoticeId(), notice.getTitle(), notice.getCreatedAt());
    }
}

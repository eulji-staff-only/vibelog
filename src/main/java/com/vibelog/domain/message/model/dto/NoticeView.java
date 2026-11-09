package com.vibelog.domain.message.model.dto;

import com.vibelog.domain.message.model.entity.Notice;

import java.time.LocalDateTime;

/* 공지 목록·상세 (U-25·U-26·A-10). 배너 값은 운영자 목록에서만 쓴다 */
public record NoticeView(Long noticeId, String title, String body, LocalDateTime createdAt, boolean bannerShown,
                         LocalDateTime bannerStartsAt, LocalDateTime bannerEndsAt) {

    public static NoticeView from(Notice notice) {
        return new NoticeView(notice.getNoticeId(), notice.getTitle(), notice.getBody(), notice.getCreatedAt(),
                notice.isBannerShown(), notice.getBannerStartsAt(), notice.getBannerEndsAt());
    }
}

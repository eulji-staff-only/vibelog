package com.vibelog.domain.message.model.entity;

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
 * [Notice]
 * 역할 : 공지 테이블(notice)과 매핑된다. 배너 노출 여부·기간을 함께 가진다 (FR-NTF-003·004·005).
 * 책임 : 컬럼은 sql/01-schema.sql 의 notice 그대로 둔다. 배너는 동시에 1개(PL-NTF-003) 검사는 #186 에서 붙인다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;

    private Long adminMemberId;

    private String title;

    private String body;

    private boolean isBannerShown;

    private LocalDateTime bannerStartsAt;

    private LocalDateTime bannerEndsAt;

    private Notice(Long adminMemberId, String title, String body, boolean isBannerShown,
                   LocalDateTime bannerStartsAt, LocalDateTime bannerEndsAt) {
        this.adminMemberId = adminMemberId;
        this.title = title;
        this.body = body;
        this.isBannerShown = isBannerShown;
        this.bannerStartsAt = bannerStartsAt;
        this.bannerEndsAt = bannerEndsAt;
    }

    public static Notice create(Long adminMemberId, String title, String body, boolean isBannerShown,
                                LocalDateTime bannerStartsAt, LocalDateTime bannerEndsAt) {
        return new Notice(adminMemberId, title, body, isBannerShown, bannerStartsAt, bannerEndsAt);
    }

    /* API-NTF-08 공지 수정. 작성한 운영자는 그대로 둔다 */
    public void update(String title, String body, boolean isBannerShown, LocalDateTime bannerStartsAt, LocalDateTime bannerEndsAt) {
        this.title = title;
        this.body = body;
        this.isBannerShown = isBannerShown;
        this.bannerStartsAt = bannerStartsAt;
        this.bannerEndsAt = bannerEndsAt;
    }
}

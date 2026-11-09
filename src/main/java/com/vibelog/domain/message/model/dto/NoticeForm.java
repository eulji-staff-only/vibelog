package com.vibelog.domain.message.model.dto;

import com.vibelog.domain.message.model.entity.Notice;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/*
 * 공지 작성·수정 폼 (API-NTF-07·08 form title, body, isBannerShown, bannerStartsAt, bannerEndsAt).
 * 배너면 시작·끝이 다 있고 끝이 시작보다 뒤여야 한다 (ck_notice_banner). 기간 겹침은 DB 를 봐야 해서 NoticeService 가 본다.
 * isBannerShown 은 체크박스라 안 고르면 값이 오지 않는다 (null = 배너 아님).
 */
@Getter
@Setter
@NoArgsConstructor
public class NoticeForm {

    public static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm";

    @NotBlank(message = "제목을 입력해 주세요")
    @Size(max = 100, message = "제목은 100자까지 쓸 수 있습니다")
    private String title;

    @NotBlank(message = "본문을 입력해 주세요")
    private String body;

    private Boolean isBannerShown;

    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime bannerStartsAt;

    @DateTimeFormat(pattern = DATETIME_PATTERN)
    private LocalDateTime bannerEndsAt;

    @AssertTrue(message = "배너 기간은 시작과 끝을 모두 정하고, 끝을 시작보다 뒤로 정해 주세요")
    public boolean isBannerPeriodValid() {
        return !bannerShown() || (bannerStartsAt != null && bannerEndsAt != null && bannerEndsAt.isAfter(bannerStartsAt));
    }

    public boolean bannerShown() {
        return Boolean.TRUE.equals(isBannerShown);
    }

    /* 수정 폼을 채울 때 */
    public static NoticeForm from(Notice notice) {
        NoticeForm form = new NoticeForm();
        form.title = notice.getTitle();
        form.body = notice.getBody();
        form.isBannerShown = notice.isBannerShown();
        form.bannerStartsAt = notice.getBannerStartsAt();
        form.bannerEndsAt = notice.getBannerEndsAt();
        return form;
    }
}

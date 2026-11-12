package com.vibelog.domain.message.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dao.NoticeRepository;
import com.vibelog.domain.message.model.dao.NotificationBulkWriter;
import com.vibelog.domain.message.model.dto.NoticeBanner;
import com.vibelog.domain.message.model.dto.NoticeForm;
import com.vibelog.domain.message.model.dto.NoticeView;
import com.vibelog.domain.message.model.entity.Notice;
import com.vibelog.domain.message.model.entity.NotificationTargetType;
import com.vibelog.domain.message.model.entity.NotificationType;
import com.vibelog.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/*
 * [NoticeService]
 * 역할 : 공지 목록·상세, 운영자 공지 작성·수정, 헤더 배너 (API-NTF-04~08, FR-NTF-003~005, PL-NTF-003).
 * 책임 : - 배너는 동시에 1개: 기간이 다른 배너 공지와 겹치면 BANNER_PERIOD_OVERLAP. 수정할 때는 자기 자신을 뺀다.
 *        - 배너가 아니면 기간을 비워 저장한다 (ck_notice_banner 는 배너일 때만 기간을 본다).
 *        - 작성할 때만 탈퇴하지 않은 회원 전체에 NOTICE 알림 1건씩. 수정은 다시 알리지 않는다 (명세 결정 9).
 *        - 삭제는 요구사항·화면에 없어 만들지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    static final int PAGE_SIZE = 10;

    private final NoticeRepository noticeRepository;
    private final NotificationBulkWriter notificationBulkWriter;
    private final MemberReader memberReader;

    public Page<NoticeView> getPage(int page) {
        return noticeRepository.findPageByOrderByCreatedAtDescNoticeIdDesc(PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE))
                .map(NoticeView::from);
    }

    public NoticeView get(Long noticeId) {
        return NoticeView.from(find(noticeId));
    }

    public NoticeForm getForm(Long noticeId) {
        return NoticeForm.from(find(noticeId));
    }

    /* 헤더 배너. 지금 기간 안의 배너 공지 1개, 없으면 null */
    public NoticeBanner getShowingBanner() {
        return noticeRepository.findShowingBanners(LocalDateTime.now(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(notice -> new NoticeBanner(notice.getNoticeId(), notice.getTitle()))
                .orElse(null);
    }

    @Transactional
    public Long create(Long adminMemberId, NoticeForm form) {
        checkBannerOverlap(form, null);
        Notice notice = noticeRepository.save(Notice.create(adminMemberId, form.getTitle().strip(), form.getBody().strip(),
                form.bannerShown(), bannerStartsAt(form), bannerEndsAt(form)));
        String content = NotificationContractService.fit("새 공지사항: %s", notice.getTitle());
        // 회원이 1만 명이면 한 줄씩 INSERT 1만 번이라, 1,000명씩 묶어 넣는다 (#482)
        notificationBulkWriter.insertAll(memberReader.findActiveMemberIds(), NotificationType.NOTICE,
                NotificationTargetType.NOTICE, notice.getNoticeId(), content);
        return notice.getNoticeId();
    }

    @Transactional
    public void update(Long noticeId, NoticeForm form) {
        Notice notice = find(noticeId);
        checkBannerOverlap(form, noticeId);
        notice.update(form.getTitle().strip(), form.getBody().strip(), form.bannerShown(), bannerStartsAt(form), bannerEndsAt(form));
    }

    private void checkBannerOverlap(NoticeForm form, Long excludeId) {
        if (form.bannerShown() && noticeRepository.existsBannerOverlap(form.getBannerStartsAt(), form.getBannerEndsAt(), excludeId)) {
            throw new BusinessException(MessageErrorCode.BANNER_PERIOD_OVERLAP);
        }
    }

    private LocalDateTime bannerStartsAt(NoticeForm form) {
        return form.bannerShown() ? form.getBannerStartsAt() : null;
    }

    private LocalDateTime bannerEndsAt(NoticeForm form) {
        return form.bannerShown() ? form.getBannerEndsAt() : null;
    }

    private Notice find(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.NOTICE_NOT_FOUND));
    }
}

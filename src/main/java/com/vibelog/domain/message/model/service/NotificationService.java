package com.vibelog.domain.message.model.service;

import com.vibelog.domain.community.model.service.CommunityReader;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dao.NotificationRepository;
import com.vibelog.domain.message.model.dto.NotificationItem;
import com.vibelog.domain.message.model.dto.NotificationPage;
import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationType;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/*
 * [NotificationService]
 * 역할 : 사이트 안 알림 목록·열기·모두 읽음, 90일 지난 알림 삭제 (API-NTF-01~03, FR-NTF-001, PL-NTF-001·002).
 * 책임 : - 알림은 받은 회원만 연다 (NOT_OWNER). 열면 읽음을 남기고 명세 "알림 도착지" 주소를 돌려준다.
 *        - 대상이 지워졌는지는 보지 않는다. 도착한 화면이 자기 "없음" 처리를 한다 (명세 메모).
 *        - 알림을 남기는 일은 계약 ⑬ NotificationContractService 가 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    static final int PAGE_SIZE = 20;
    static final int KEEP_DAYS = 90;

    private final NotificationRepository notificationRepository;
    private final CommunityReader communityReader;

    public NotificationPage getPage(Long memberId, int page) {
        Slice<Notification> slice = notificationRepository.findByMemberIdOrderByCreatedAtDescNotificationIdDesc(
                memberId, PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE));
        return new NotificationPage(countUnread(memberId), slice.hasNext(),
                slice.map(NotificationItem::from).getContent());
    }

    /* M-10 배지 획득 축하 (contest 명세): 안 읽은 BADGE 알림이 있으면 가장 최근 것. "확인"은 그 알림 열기(/notifications/{id}) */
    public Optional<NotificationItem> findUnreadBadge(Long memberId) {
        return notificationRepository.findFirstByMemberIdAndNotificationTypeAndReadAtIsNullOrderByNotificationIdDesc(memberId, NotificationType.BADGE)
                .map(NotificationItem::from);
    }

    public long countUnread(Long memberId) {
        return notificationRepository.countByMemberIdAndReadAtIsNull(memberId);
    }

    /* API-NTF-02. 읽음을 남기고 갈 주소를 돌려준다 */
    @Transactional
    public String open(Long memberId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isOwnedBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        notification.read();
        return destination(notification);
    }

    @Transactional
    public void readAll(Long memberId) {
        notificationRepository.markAllRead(memberId, LocalDateTime.now());
    }

    /* PL-NTF-002. 하루 한 번 새벽 4시 (@EnableScheduling 은 global/config/SchedulingConfig) */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public int deleteOld() {
        return notificationRepository.deleteCreatedBefore(LocalDateTime.now().minusDays(KEEP_DAYS));
    }

    /* 명세 "알림 도착지" 표 */
    private String destination(Notification notification) {
        Long targetId = notification.getTargetId();
        return switch (notification.getNotificationType()) {
            case RECOMMEND -> "/community/" + targetId;
            // 댓글은 지워도 행이 남는다. 행이 아예 없을 때만 E-01
            case COMMENT, REPLY -> communityReader.findPostIdOfComment(targetId)
                    .map(postId -> "/community/" + postId + "#comment-" + targetId)
                    .orElseThrow(() -> new BusinessException(MessageErrorCode.NOTIFICATION_NOT_FOUND));
            case MESSAGE -> "/messages?box=received&messageId=" + targetId;
            case BADGE -> "/mypage?tab=badges";
            case NOTICE -> "/notices/" + targetId;
            // ponytail: REVIEW 는 동료 리뷰(#183)가 생겨야 남는다. 그때 CommunityReader 로 portfolioId 를 받아 /portfolios/{id} 로 보낸다
            // HIDDEN 은 P3
            case REVIEW, HIDDEN -> throw new BusinessException(MessageErrorCode.NOTIFICATION_NOT_FOUND);
        };
    }
}

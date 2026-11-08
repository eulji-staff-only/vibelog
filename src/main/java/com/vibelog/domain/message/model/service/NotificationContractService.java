package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.dao.NotificationRepository;
import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationTargetType;
import com.vibelog.domain.message.model.entity.NotificationType;
import com.vibelog.domain.message.model.policy.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/*
 * [NotificationContractService]
 * 역할 : message 영역이 다른 영역에 주는 계약 ⑬(알림 보내기)의 진짜 구현.
 * 책임 : - 다른 영역은 이 클래스가 아니라 model/policy/NotificationSender 를 주입해 쓴다.
 *        - 기록 계약이라 부르는 쪽 트랜잭션에 함께 묶인다.
 *        - 문구는 notification.content(255자)에 맞춘다. 넘치면 제목을 줄이고 "…"를 붙인다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationContractService implements NotificationSender {

    static final int CONTENT_MAX = 255;

    private final NotificationRepository notificationRepository;

    @Override
    public void recommended(Long actorMemberId, Long receiverMemberId, Long postId, String postTitle) {
        save(actorMemberId, receiverMemberId, NotificationType.RECOMMEND, NotificationTargetType.POST, postId,
                "내 글 「%s」이 추천을 받았습니다", postTitle);
    }

    @Override
    public void commented(Long actorMemberId, Long receiverMemberId, Long commentId, String postTitle) {
        save(actorMemberId, receiverMemberId, NotificationType.COMMENT, NotificationTargetType.COMMENT, commentId,
                "내 글 「%s」에 댓글이 달렸습니다", postTitle);
    }

    @Override
    public void replied(Long actorMemberId, Long receiverMemberId, Long commentId, String postTitle) {
        save(actorMemberId, receiverMemberId, NotificationType.REPLY, NotificationTargetType.COMMENT, commentId,
                "내 댓글에 답글이 달렸습니다: 「%s」", postTitle);
    }

    @Override
    public void reviewed(Long actorMemberId, Long receiverMemberId, Long peerReviewId, String portfolioTitle) {
        save(actorMemberId, receiverMemberId, NotificationType.REVIEW, NotificationTargetType.REVIEW, peerReviewId,
                "내 작품 「%s」에 리뷰가 달렸습니다", portfolioTitle);
    }

    @Override
    public void badgeAwarded(Long receiverMemberId, Long contestAwardId, String badgeName) {
        save(null, receiverMemberId, NotificationType.BADGE, NotificationTargetType.CONTEST_AWARD, contestAwardId,
                "배지를 받았습니다: %s", badgeName);
    }

    private void save(Long actorMemberId, Long receiverMemberId, NotificationType type, NotificationTargetType targetType,
                      Long targetId, String format, String title) {
        if (Objects.equals(actorMemberId, receiverMemberId)) {
            return;
        }
        notificationRepository.save(Notification.create(receiverMemberId, type, targetType, targetId, fit(format, title)));
    }

    /* 문구가 255자를 넘으면 제목 끝을 잘라 "…"로 맞춘다 (형식 문자열 부분은 그대로 둔다) */
    static String fit(String format, String title) {
        String content = format.formatted(title);
        if (content.length() <= CONTENT_MAX) {
            return content;
        }
        int keep = title.length() - (content.length() - CONTENT_MAX) - 1;
        return format.formatted(title.substring(0, Math.max(keep, 0)) + "…");
    }
}

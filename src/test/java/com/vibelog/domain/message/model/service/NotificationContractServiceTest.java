package com.vibelog.domain.message.model.service;

import com.vibelog.domain.message.model.dao.NoticeRepository;
import com.vibelog.domain.message.model.dao.NotificationRepository;
import com.vibelog.domain.message.model.dto.NoticeSummary;
import com.vibelog.domain.message.model.entity.Notice;
import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationTargetType;
import com.vibelog.domain.message.model.entity.NotificationType;
import com.vibelog.domain.message.model.policy.NotificationSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/*
 * message 계약 ⑬(알림 보내기)과 MessageReader 를 실제 DB(01·02 실행된 로컬 MySQL)로 확인한다.
 * 회원은 02-seed 테스트 계정(1 학습자, 2 운영자)을 쓴다. 끝나면 모두 롤백된다.
 */
@SpringBootTest
@Transactional
class NotificationContractServiceTest {

    static final long LEARNER = 1L;
    static final long OTHER = 2L;

    @Autowired NotificationSender notificationSender;
    @Autowired NotificationRepository notificationRepository;
    @Autowired MessageReader messageReader;
    @Autowired NoticeRepository noticeRepository;

    @Test
    void 계약13_추천하면_글_작성자에게_추천_알림이_남는다() {
        notificationSender.recommended(OTHER, LEARNER, 10L, "내 첫 작품");

        Notification n = onlyNotificationOf(LEARNER);
        assertThat(n.getNotificationType()).isEqualTo(NotificationType.RECOMMEND);
        assertThat(n.getTargetType()).isEqualTo(NotificationTargetType.POST);
        assertThat(n.getTargetId()).isEqualTo(10L);
        assertThat(n.getContent()).isEqualTo("내 글 「내 첫 작품」이 추천을 받았습니다");
        assertThat(n.getReadAt()).isNull();
    }

    @Test
    void 계약13_종류마다_대상이_정해진다() {
        notificationSender.commented(OTHER, LEARNER, 21L, "글");
        notificationSender.replied(OTHER, LEARNER, 22L, "글");
        notificationSender.reviewed(OTHER, LEARNER, 23L, "작품");
        notificationSender.badgeAwarded(LEARNER, 24L, "2026년 11월 1등");

        assertThat(notificationRepository.findAll()).filteredOn(n -> n.getMemberId() == LEARNER)
                .extracting(Notification::getNotificationType, Notification::getTargetType, Notification::getTargetId)
                .containsExactlyInAnyOrder(
                        tuple(NotificationType.COMMENT, NotificationTargetType.COMMENT, 21L),
                        tuple(NotificationType.REPLY, NotificationTargetType.COMMENT, 22L),
                        tuple(NotificationType.REVIEW, NotificationTargetType.REVIEW, 23L),
                        tuple(NotificationType.BADGE, NotificationTargetType.CONTEST_AWARD, 24L));
    }

    @Test
    void 계약13_내가_한_일은_나에게_알리지_않는다() {
        notificationSender.commented(LEARNER, LEARNER, 30L, "내 글");

        assertThat(notificationRepository.findAll()).noneMatch(n -> n.getMemberId() == LEARNER);
    }

    @Test
    void 문구가_255자를_넘으면_제목을_줄인다() {
        String content = NotificationContractService.fit("내 글 「%s」이 추천을 받았습니다", "가".repeat(300));

        assertThat(content).hasSize(NotificationContractService.CONTENT_MAX).endsWith("…」이 추천을 받았습니다");
        assertThat(NotificationContractService.fit("배지를 받았습니다: %s", "짧은 이름")).isEqualTo("배지를 받았습니다: 짧은 이름");
    }

    @Test
    void 최근_공지는_최신순으로_limit_개() {
        Notice first = noticeRepository.save(Notice.create(OTHER, "첫 공지", "본문", false, null, null));
        Notice second = noticeRepository.save(Notice.create(OTHER, "둘째 공지", "본문", false, null, null));

        List<NoticeSummary> recent = messageReader.findRecentNotices(1);

        assertThat(recent).extracting(NoticeSummary::noticeId).containsExactly(second.getNoticeId());
        assertThat(messageReader.findRecentNotices(10)).extracting(NoticeSummary::noticeId)
                .containsSubsequence(second.getNoticeId(), first.getNoticeId());
    }

    private Notification onlyNotificationOf(long memberId) {
        List<Notification> mine = notificationRepository.findAll().stream().filter(n -> n.getMemberId() == memberId).toList();
        assertThat(mine).hasSize(1);
        return mine.get(0);
    }
}

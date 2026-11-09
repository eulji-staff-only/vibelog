package com.vibelog.domain.message.model.service;

import com.vibelog.domain.auth.model.service.MemberReader;
import com.vibelog.domain.message.exception.MessageErrorCode;
import com.vibelog.domain.message.model.dao.MessageRepository;
import com.vibelog.domain.message.model.dao.NotificationRepository;
import com.vibelog.domain.message.model.dto.MessageItem;
import com.vibelog.domain.message.model.dto.MessageView;
import com.vibelog.domain.message.model.dto.ReceiverItem;
import com.vibelog.domain.message.model.entity.Message;
import com.vibelog.domain.message.model.entity.Notification;
import com.vibelog.domain.message.model.entity.NotificationTargetType;
import com.vibelog.domain.message.model.entity.NotificationType;
import com.vibelog.global.exception.BusinessException;
import com.vibelog.global.exception.CommonErrorCode;
import com.vibelog.global.policy.BannedWordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
 * [MessageService]
 * 역할 : 쪽지함·쪽지 열기·받는 사람 찾기·보내기·답장, 1년 지난 쪽지 삭제 (API-MSG-01~05, FR-MSG-001·002, PL-MSG-001·003).
 * 책임 : - 쪽지는 주고받은 두 사람만 연다 (NOT_OWNER). 받은 사람이 열면 읽음을 남긴다.
 *        - 보내기 세 경로(쪽지함·공개 프로필·답장)가 send 하나를 쓴다. 검사 순서는 명세 그대로:
 *          금칙어 → 이메일 인증 → 나에게 보냄 → 받는 사람(없음·탈퇴) → 하루 20통 → 연속 3통. 입력 길이는 Controller 의 @Valid.
 *        - 보내면 받는 사람에게 MESSAGE 알림 (message 영역 안이라 계약 ⑬을 거치지 않는다).
 *        - 각자 지우기(sender_deleted_at·receiver_deleted_at)와 차단은 이번 범위 밖이다 (명세 메모).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    public static final String BOX_RECEIVED = "received";
    public static final String BOX_SENT = "sent";
    static final int PAGE_SIZE = 20;
    static final int RECEIVER_SEARCH_LIMIT = 10;
    static final int DAILY_LIMIT = 20;
    static final int UNANSWERED_LIMIT = 3;
    static final int KEEP_YEARS = 1;

    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final MemberReader memberReader;
    private final BannedWordPolicy bannedWordPolicy;

    /* box 가 sent 가 아니면 받은 쪽지함 */
    public Page<MessageItem> getBox(Long memberId, String box, int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE);
        boolean sent = BOX_SENT.equals(box);
        Page<Message> messages = sent
                ? messageRepository.findBySenderMemberIdOrderByCreatedAtDescMessageIdDesc(memberId, pageable)
                : messageRepository.findByReceiverMemberIdOrderByCreatedAtDescMessageIdDesc(memberId, pageable);
        Map<Long, String> nicknames = memberReader.getNicknames(
                messages.map(m -> sent ? m.getReceiverMemberId() : m.getSenderMemberId()).toSet());
        return messages.map(m -> {
            Long other = sent ? m.getReceiverMemberId() : m.getSenderMemberId();
            return new MessageItem(m.getMessageId(), other, nicknames.get(other), MessageItem.preview(m.getContent()),
                    m.getCreatedAt(), m.getReadAt() != null);
        });
    }

    /* API-MSG-01 messageId. 받은 쪽지면 읽음을 남긴다 */
    @Transactional
    public MessageView open(Long memberId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND));
        if (!message.isBetween(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        if (message.isReceivedBy(memberId)) {
            message.read();
        }
        Map<Long, String> nicknames = memberReader.getNicknames(List.of(message.getSenderMemberId(), message.getReceiverMemberId()));
        return new MessageView(message.getMessageId(), message.getSenderMemberId(), nicknames.get(message.getSenderMemberId()),
                message.getReceiverMemberId(), nicknames.get(message.getReceiverMemberId()), message.getContent(),
                message.getCreatedAt(), message.isReceivedBy(memberId));
    }

    /* API-MSG-02. 비어 있으면 빈 목록 */
    public List<ReceiverItem> findReceivers(Long memberId, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return List.of();
        }
        return memberReader.findActiveNicknamesStartingWith(nickname.strip(), memberId, RECEIVER_SEARCH_LIMIT).entrySet().stream()
                .map(entry -> new ReceiverItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    /* 화면에 받는 사람 닉네임을 다시 보여 줄 때 (입력 오류로 폼을 다시 그릴 때) */
    public String getNickname(Long memberId) {
        return memberId == null ? null : memberReader.getNickname(memberId);
    }

    /* API-MSG-03·04. 새 쪽지 ID 를 돌려준다 */
    @Transactional
    public Long send(Long senderMemberId, Long receiverMemberId, String content) {
        return save(senderMemberId, receiverMemberId, null, content);
    }

    /* API-MSG-05. 내가 받은 쪽지에만 답한다. 받는 사람은 원래 보낸 사람 */
    @Transactional
    public Long reply(Long memberId, Long messageId, String content) {
        Message original = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND));
        if (!original.isReceivedBy(memberId)) {
            throw new BusinessException(CommonErrorCode.NOT_OWNER);
        }
        return save(memberId, original.getSenderMemberId(), messageId, content);
    }

    /* PL-MSG-003. 하루 한 번 새벽 4시 30분 */
    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public int deleteOld() {
        // ponytail: 지울 ID 를 한 번에 메모리로 읽는다. 하루 쌓이는 양이 수만 건을 넘으면 1,000개씩 나눠 지운다
        List<Long> ids = messageRepository.findIdsCreatedBefore(LocalDateTime.now().minusYears(KEEP_YEARS));
        if (ids.isEmpty()) {
            return 0;
        }
        messageRepository.clearParentIn(ids);
        return messageRepository.deleteByIdIn(ids);
    }

    private Long save(Long senderMemberId, Long receiverMemberId, Long parentMessageId, String content) {
        bannedWordPolicy.check(content);
        if (!memberReader.isEmailVerified(senderMemberId)) {
            throw new BusinessException(CommonErrorCode.EMAIL_NOT_VERIFIED);
        }
        if (senderMemberId.equals(receiverMemberId)) {
            throw new BusinessException(MessageErrorCode.MESSAGE_SELF);
        }
        if (!memberReader.isActive(receiverMemberId)) {
            throw new BusinessException(MessageErrorCode.MESSAGE_RECEIVER_NOT_FOUND);
        }
        if (messageRepository.countBySenderMemberIdAndCreatedAtGreaterThanEqual(senderMemberId, LocalDate.now().atStartOfDay())
                >= DAILY_LIMIT) {
            throw new BusinessException(MessageErrorCode.MESSAGE_DAILY_LIMIT);
        }
        long lastAnswerId = messageRepository.findFirstBySenderMemberIdAndReceiverMemberIdOrderByMessageIdDesc(receiverMemberId, senderMemberId)
                .map(Message::getMessageId)
                .orElse(0L);
        if (messageRepository.countBySenderMemberIdAndReceiverMemberIdAndMessageIdGreaterThan(senderMemberId, receiverMemberId, lastAnswerId)
                >= UNANSWERED_LIMIT) {
            throw new BusinessException(MessageErrorCode.MESSAGE_UNANSWERED_LIMIT);
        }
        Message message = messageRepository.save(Message.create(senderMemberId, receiverMemberId, parentMessageId, content.strip()));
        notificationRepository.save(Notification.create(receiverMemberId, NotificationType.MESSAGE, NotificationTargetType.MESSAGE,
                message.getMessageId(), memberReader.getNickname(senderMemberId) + "님이 쪽지를 보냈습니다"));
        return message.getMessageId();
    }
}

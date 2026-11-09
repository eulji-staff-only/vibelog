package com.vibelog.domain.message.model.dao;

import com.vibelog.domain.message.model.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /* 쪽지함 받은·보낸 탭 (API-MSG-01). 최신순, 같은 시각이면 ID 큰 것 먼저 */
    Page<Message> findByReceiverMemberIdOrderByCreatedAtDescMessageIdDesc(Long receiverMemberId, Pageable pageable);

    Page<Message> findBySenderMemberIdOrderByCreatedAtDescMessageIdDesc(Long senderMemberId, Pageable pageable);

    /* 하루 20통 (PL-MSG-003): since(오늘 0시) 이후 내가 보낸 수 */
    long countBySenderMemberIdAndCreatedAtGreaterThanEqual(Long senderMemberId, LocalDateTime since);

    /* 연속 3통: 상대가 나에게 보낸 마지막 쪽지 */
    Optional<Message> findFirstBySenderMemberIdAndReceiverMemberIdOrderByMessageIdDesc(Long senderMemberId, Long receiverMemberId);

    /* 연속 3통: 그 쪽지(afterMessageId) 이후 내가 상대에게 보낸 수. ID 는 저장 순서대로 커진다 */
    long countBySenderMemberIdAndReceiverMemberIdAndMessageIdGreaterThan(Long senderMemberId, Long receiverMemberId,
                                                                        Long afterMessageId);

    /* 1년 지난 쪽지 삭제 (PL-MSG-003) */
    @Query("select m.messageId from Message m where m.createdAt < :before")
    List<Long> findIdsCreatedBefore(@Param("before") LocalDateTime before);

    /* 지울 쪽지를 가리키는 답장의 parent 를 먼저 비운다 (fk_message_parent_message_id 가 RESTRICT) */
    @Modifying(clearAutomatically = true)
    @Query("update Message m set m.parentMessageId = null where m.parentMessageId in :ids")
    int clearParentIn(@Param("ids") Collection<Long> ids);

    @Modifying(clearAutomatically = true)
    @Query("delete from Message m where m.messageId in :ids")
    int deleteByIdIn(@Param("ids") Collection<Long> ids);
}
